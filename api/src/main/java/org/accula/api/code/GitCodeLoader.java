package org.accula.api.code;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import org.accula.api.code.git.Git;
import org.accula.api.code.git.Git.Repo;
import org.accula.api.code.git.GitCommit;
import org.accula.api.code.git.GitDiffEntry;
import org.accula.api.code.git.GitDiffEntry.Addition;
import org.accula.api.code.git.GitDiffEntry.Deletion;
import org.accula.api.code.git.GitDiffEntry.Modification;
import org.accula.api.code.git.GitDiffEntry.Renaming;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.git.GitFileChanges;
import org.accula.api.code.git.GitRefs;
import org.accula.api.code.git.Identifiable;
import org.accula.api.code.git.Snippet;
import org.accula.api.code.lines.LineSet;
import org.accula.api.converter.CodeToModelConverter;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import org.accula.api.util.Checks;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class GitCodeLoader implements CodeLoader {
    private final GitCredentialsProvider credentialsProvider;
    private final Git git;

    @Override
    public Flux<FileEntity<Snapshot>> loadFiles(final GithubRepo repo, final Iterable<Snapshot> snapshots, final FileFilter filter) {
        final var snapshotMap = snapshotMap(snapshots);
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono
                        .fromFuture(gitRepo.fileChanges(snapshotMap.keySet()))
                        .flatMap(changesToSha -> loadFiles(changesToSha, filter, gitRepo, snapshotMap)))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<FileEntity<Snapshot>> loadSnippets(final Snapshot snapshot, final List<SnippetMarker> markers) {
        return withCommonGitRepo(snapshot)
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.sha()))
                        .map(files -> convertSnippets(files, markers))
                        .flatMap(snippets -> Mono
                                .fromFuture(repo.catFiles(snippets))
                                .map(filesContent -> snippets
                                        .stream()
                                        .map(snippet -> new FileEntity<>(
                                                snapshot,
                                                snippet.file().name(),
                                                Checks.notNull(filesContent.get(snippet), "File content"),
                                                LineSet.inRange(snippet.lines()))))))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<DiffEntry<Snapshot>> loadDiff(final Snapshot base,
                                              final Snapshot head,
                                              final int minSimilarityIndex,
                                              final FileFilter filter) {
        return withCommonGitRepo(head)
                .flatMap(repo -> addOrUpdateRemote(repo, base))
                .flatMapMany(repo -> loadDiff(repo, base, head, minSimilarityIndex, filter));
    }

    @Override
    public Flux<DiffEntry<Snapshot>> loadRemoteDiff(final GithubRepo projectRepo,
                                                    final Snapshot base,
                                                    final Snapshot head,
                                                    final int minSimilarityIndex,
                                                    final FileFilter filter) {
        return withProjectGitRepo(projectRepo)
                .flatMap(repo -> addOrUpdateRemotes(repo, base, head))
                .flatMapMany(repo -> loadDiff(repo, base, head, minSimilarityIndex, filter));
    }

    @Override
    public Flux<String> loadFilenames(final GithubRepo projectRepo) {
        return withProjectGitRepo(projectRepo)
                .flatMapMany(repo -> Mono.fromFuture(repo.log(GitRefs.originHead()))
                        .flatMapMany(Flux::fromIterable)
                        .map(GitCommit::sha)
                        .collectList()
                        .flatMapMany(commits -> Mono.fromFuture(repo.show(commits)))
                        .map(Map::keySet)
                        .flatMap(Flux::fromIterable)
                        .distinct(GitFile::name)
                        .map(GitFile::name));
    }

    @Override
    public Flux<Commit> loadAllCommits(final GithubRepo repo) {
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono.fromFuture(gitRepo.revListAllPretty()))
                .flatMapMany(Flux::fromIterable)
                .map(CodeToModelConverter::convert);
    }

    @Override
    public Flux<Commit> loadCommits(final GithubRepo repo, final String sinceRefExclusive, final String untilRefInclusive) {
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono.fromFuture(gitRepo.log(sinceRefExclusive, untilRefInclusive)))
                .flatMapMany(Flux::fromIterable)
                .map(CodeToModelConverter::convert);
    }

    @Override
    public Mono<Commit> loadCommit(final GithubRepo repo, final String ref) {
        return withCommonGitRepo(repo)
            .flatMap(gitRepo -> Mono.fromFuture(gitRepo.logSingle(ref)))
            .map(CodeToModelConverter::convert);
    }

    @Override
    public Flux<Commit> loadCommits(final GithubRepo repo, final String ref) {
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono.fromFuture(gitRepo.log(ref)))
                .flatMapMany(Flux::fromIterable)
                .map(CodeToModelConverter::convert);
    }

    /// We name each common repo git folder like that: <owner-login>_<repo-name>
    private Mono<Repo> withCommonGitRepo(final GithubRepo repo) {
        final var repoGitDirectory = Path.of(repo.owner().login() + "_" + repo.name());
        return withRepoGitUrl(repo)
            .transform(Lambda.passingFirstArg(this::withGitRepo, repoGitDirectory));
    }

    private Mono<Repo> withCommonGitRepo(final Snapshot snapshot) {
        return withCommonGitRepo(snapshot.repo());
    }

    private Mono<Repo> withProjectGitRepo(final GithubRepo projectRepo) {
        final var projectGitDirectory = Path.of(projectRepo.name());
        return withRepoGitUrl(projectRepo)
            .transform(Lambda.passingFirstArg(this::withGitRepo, projectGitDirectory));
    }

    private Mono<Repo> withGitRepo(final Path directory, final Mono<String> urlMono) {
        return Mono
            .fromFuture(git.repo(directory))
            .switchIfEmpty(urlMono.flatMap(url -> Mono.fromFuture(git.clone(url, directory.toString()))))
            .flatMap(repo -> Mono.fromFuture(repo.fetch()));
    }

    private Mono<String> withRepoGitUrl(final GithubRepo repo) {
        if (!repo.isPrivate()) {
            return Mono.just(repoGitUrl(repo));
        }
        return credentialsProvider
            .gitCredentials(repo.id())
            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Git credentials MUST be present for private repos")))
            .map(credentials -> "https://%s:%s@github.com/%s/%s.git"
                .formatted(credentials.login(), credentials.accessToken(), repo.owner().login(), repo.name()));
    }

    private static String repoGitUrl(final GithubRepo repo) {
        return "https://github.com/%s/%s.git".formatted(repo.owner().login(), repo.name());
    }

    private static Map<String, List<Snapshot>> snapshotMap(final Iterable<Snapshot> snapshots) {
        return Streams.stream(snapshots)
                .collect(Collectors.toMap(Snapshot::sha, List::<Snapshot>of, (from, to) -> {
                    if (from instanceof ArrayList<Snapshot> arrayList) {
                        if (to.size() == 1) {
                            arrayList.add(to.get(0));
                        } else {
                            assert false;
                            arrayList.addAll(to);
                        }
                        return arrayList;
                    }
                    final var arrayList = new ArrayList<Snapshot>();
                    arrayList.add(from.get(0));
                    arrayList.add(to.get(0));
                    return arrayList;
                }));
    }

    private static Mono<Stream<FileEntity<Snapshot>>> loadFiles(final Map<GitFileChanges, String> changesToSha,
                                                                final FileFilter filter,
                                                                final Repo repo,
                                                                final Map<String, List<Snapshot>> snapshotMap) {
        changesToSha.entrySet().removeIf(entry -> !filter.test(entry.getKey().file().name()));
        return Mono
                .fromFuture(repo.catFiles(changesToSha.keySet()))
                .map(filesContent -> convertFiles(changesToSha, snapshotMap, filesContent));
    }

    private static Stream<FileEntity<Snapshot>> convertFiles(final Map<GitFileChanges, String> changesToSha,
                                                             final Map<String, List<Snapshot>> snapshotMap,
                                                             final Map<GitFileChanges, String> filesContent) {
        return changesToSha
                .entrySet()
                .stream()
                .flatMap(fileChangesAndCommitSha -> {
                    final var fileChanges = fileChangesAndCommitSha.getKey();
                    final var commitSha = fileChangesAndCommitSha.getValue();
                    final var fileContent = Checks.notNull(filesContent.get(fileChanges), "File content");
                    final var snapshot = snapshotMap.get(commitSha);
                    return convertFiles(snapshot, fileChanges, fileContent);
                });
    }

    private static Stream<FileEntity<Snapshot>> convertFiles(final List<Snapshot> snapshots,
                                                             final GitFileChanges fileChanges,
                                                             final String fileContent) {
        return snapshots
                .stream()
                .map(snapshot -> new FileEntity<>(
                        snapshot,
                        fileChanges.file().name(),
                        fileContent,
                        fileChanges.changedLines()
                ));
    }

    private static Mono<Repo> addOrUpdateRemotes(final Repo repo, final Snapshot base, final Snapshot head) {
        final var baseRemote = base.repo().owner().login();
        final var headRemote = head.repo().owner().login();
        final var baseUrl = repoGitUrl(base.repo());
        final var headUrl = repoGitUrl(head.repo());
        return Mono
                .fromFuture(repo.remote())
                .flatMap(remotesPresent -> Mono
                        .zip(
                                addOrUpdateRemote(repo, baseUrl, baseRemote, remotesPresent),
                                addOrUpdateRemote(repo, headUrl, headRemote, remotesPresent),
                                Lambda.firstArg()
                        ));
    }

    private static Mono<Repo> addOrUpdateRemote(final Repo repo, final Snapshot remote) {
        final var url = repoGitUrl(remote.repo());
        final var name = remote.repo().owner().login();
        return Mono
                .fromFuture(repo.remote())
                .flatMap(remotesPresent -> addOrUpdateRemote(repo, url, name, remotesPresent));
    }

    private static Mono<Repo> addOrUpdateRemote(final Repo repo,
                                                final String remoteUrl,
                                                final String remote,
                                                final Set<String> remotesPresent) {
        return Mono.fromFuture(remotesPresent.contains(remote) ? repo.remoteUpdate(remote) : repo.remoteAdd(remoteUrl, remote));
    }

    private static Flux<DiffEntry<Snapshot>> loadDiff(final Repo repo,
                                                      final Snapshot base,
                                                      final Snapshot head,
                                                      final int findRenamesMinSimilarityIndex,
                                                      final FileFilter filter) {
        return Mono
                .fromFuture(repo.diff(base.sha(), head.sha(), findRenamesMinSimilarityIndex))
                .map(diffEntries -> diffEntries
                        .stream()
                        .filter(entry -> entry.passes(filter))
                        .toList())
                .flatMapMany(diffEntries -> Mono
                        .fromFuture(repo
                                .catFiles(diffEntries
                                        .stream()
                                        .flatMap(GitDiffEntry::objectIds)
                                        .toList()))
                        .transform(convertDiffEntries(diffEntries, base, head))
                        .flatMapMany(Flux::fromStream));
    }

    private static Function<Mono<Map<Identifiable, String>>, Mono<Stream<DiffEntry<Snapshot>>>>
    convertDiffEntries(final List<GitDiffEntry> diffEntries, final Snapshot base, final Snapshot head) {
        return filesMono -> filesMono
                .map(files -> diffEntries
                        .stream()
                        .map(diffEntry -> {
                            if (diffEntry instanceof Addition addition) {
                                return DiffEntry.of(
                                        FileEntity.absent(base),
                                        new FileEntity<>(head, addition.head().name(), files.get(addition.head()), LineSet.all())
                                );
                            }
                            if (diffEntry instanceof Deletion deletion) {
                                return DiffEntry.of(
                                        new FileEntity<>(base, deletion.base().name(), files.get(deletion.base()), LineSet.all()),
                                        FileEntity.absent(head)
                                );
                            }
                            if (diffEntry instanceof Modification modification) {
                                return DiffEntry.of(
                                        new FileEntity<>(base, modification.base().name(), files.get(modification.base()), LineSet.all()),
                                        new FileEntity<>(head, modification.head().name(), files.get(modification.head()), LineSet.all())
                                );
                            }
                            if (diffEntry instanceof Renaming renaming) {
                                return new DiffEntry<>(
                                        new FileEntity<>(base, renaming.base().name(), files.get(renaming.base()), LineSet.all()),
                                        new FileEntity<>(head, renaming.head().name(), files.get(renaming.head()), LineSet.all()),
                                        renaming.similarityIndex()
                                );
                            }

                            throw new IllegalStateException("Unexpected diffEntry class: " + diffEntry.getClass().getName());
                        }));
    }

    private static List<Snippet> convertSnippets(final List<GitFile> files, final List<SnippetMarker> markers) {
        final var nameToFileMap = files
                .stream()
                .collect(toMap(GitFile::name, Function.identity()));
        return markers
                .stream()
                .map(marker -> {
                    final var file = nameToFileMap.get(marker.filename());
                    if (file == null) {
                        return null;
                    }
                    return Snippet.of(file, marker.lines());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
