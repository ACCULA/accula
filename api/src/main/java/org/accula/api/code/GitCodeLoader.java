package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.git.Git;
import org.accula.api.code.git.Git.Repo;
import org.accula.api.code.git.GitDiffEntry;
import org.accula.api.code.git.GitDiffEntry.Addition;
import org.accula.api.code.git.GitDiffEntry.Deletion;
import org.accula.api.code.git.GitDiffEntry.Modification;
import org.accula.api.code.git.GitDiffEntry.Renaming;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.git.GitRefs;
import org.accula.api.code.git.Identifiable;
import org.accula.api.code.git.Snippet;
import org.accula.api.converter.GitToModelConverter;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class GitCodeLoader implements CodeLoader {
    private static final String GITHUB_BASE_URL = "https://github.com/";
    private static final String GIT_EXTENSION = ".git";

    private final Git git;

    @Override
    public Flux<FileEntity<Snapshot>> loadFiles(final Snapshot snapshot, final FileFilter filter) {
        return withCommonGitRepo(snapshot)
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.sha()))
                        .map(files -> files
                                .stream()
                                .filter(file -> filter.test(file.name()))
                                .collect(toList()))
                        .flatMap(files -> Mono
                                .fromFuture(repo.catFiles(files))
                                .map(filesContent -> files
                                        .stream()
                                        .map(file -> new FileEntity<>(snapshot, file.name(), filesContent.get(file))))))
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
                                                snapshot, snippet.file().name(),
                                                filesContent.get(snippet))))))
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
                .flatMap(repo -> Mono.fromFuture(repo.lsTree(GitRefs.originHead())))
                .flatMapMany(Flux::fromIterable)
                .map(GitFile::name);
    }

    @Override
    public Flux<Commit> loadAllCommits(final GithubRepo repo) {
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono.fromFuture(gitRepo.revListAllPretty()))
                .flatMapMany(Flux::fromIterable)
                .map(GitToModelConverter::convert);
    }

    @Override
    public Flux<Commit> loadCommits(final GithubRepo repo, final String sinceRefExclusive, final String untilRefInclusive) {
        return withCommonGitRepo(repo)
                .flatMap(gitRepo -> Mono.fromFuture(gitRepo.log(sinceRefExclusive, untilRefInclusive)))
                .flatMapMany(Flux::fromIterable)
                .map(GitToModelConverter::convert);
    }

    /// We name each common repo git folder like that: <owner-login>_<repo-name>
    private Mono<Repo> withCommonGitRepo(final GithubRepo repo) {
        final var repoGitDirectory = Path.of(repo.owner().login() + "_" + repo.name());
        final var repoUrl = repoGitUrl(repo);
        return withGitRepo(repoGitDirectory, repoUrl);
    }

    private Mono<Repo> withCommonGitRepo(final Snapshot snapshot) {
        return withCommonGitRepo(snapshot.repo());
    }

    private Mono<Repo> withProjectGitRepo(final GithubRepo projectRepo) {
        final var projectGitDirectory = Path.of(projectRepo.name());
        final var projectRepoUrl = repoGitUrl(projectRepo);
        return withGitRepo(projectGitDirectory, projectRepoUrl);
    }

    private Mono<Repo> withGitRepo(final Path directory, final String url) {
        return Mono
                .fromFuture(git.repo(directory))
                .switchIfEmpty(Mono.fromFuture(git.clone(url, directory.toString())))
                .flatMap(repo -> Mono.fromFuture(repo.fetch()));
    }

    private Mono<Repo> addOrUpdateRemotes(final Repo repo, final Snapshot base, final Snapshot head) {
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

    private Mono<Repo> addOrUpdateRemote(final Repo repo, final Snapshot remote) {
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
                        .collect(toList()))
                .flatMapMany(diffEntries -> Mono
                        .fromFuture(repo
                                .catFiles(diffEntries
                                        .stream()
                                        .flatMap(GitDiffEntry::objectIds)
                                        .collect(toList())))
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
                                        new FileEntity<>(head, addition.head().name(), files.get(addition.head()))
                                );
                            }
                            if (diffEntry instanceof Deletion deletion) {
                                return DiffEntry.of(
                                        new FileEntity<>(base, deletion.base().name(), files.get(deletion.base())),
                                        FileEntity.absent(head)
                                );
                            }
                            if (diffEntry instanceof Modification modification) {
                                return DiffEntry.of(
                                        new FileEntity<>(base, modification.base().name(), files.get(modification.base())),
                                        new FileEntity<>(head, modification.head().name(), files.get(modification.head()))
                                );
                            }
                            if (diffEntry instanceof Renaming renaming) {
                                return new DiffEntry<>(
                                        new FileEntity<>(base, renaming.base().name(), files.get(renaming.base())),
                                        new FileEntity<>(head, renaming.head().name(), files.get(renaming.head())),
                                        renaming.similarityIndex()
                                );
                            }

                            return null;
                        }));
    }

    private static List<Snippet> convertSnippets(final List<GitFile> files, final List<SnippetMarker> markers) {
        final var nameToFileMap = files
                .stream()
                .collect(toMap(GitFile::name, Lambda.identity()));
        return markers
                .stream()
                .map(marker -> {
                    final var file = nameToFileMap.get(marker.filename());
                    if (file == null) {
                        return null;
                    }
                    return Snippet.of(file, marker.fromLine(), marker.toLine());
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static String repoGitUrl(final GithubRepo repo) {
        return GITHUB_BASE_URL + repo.owner().login() + "/" + repo.name() + GIT_EXTENSION;
    }
}
