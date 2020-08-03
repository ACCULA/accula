package org.accula.api.code;

import org.accula.api.code.git.Git;
import org.accula.api.code.git.Git.Repo;
import org.accula.api.code.git.GitDiffEntry;
import org.accula.api.code.git.GitDiffEntry.Addition;
import org.accula.api.code.git.GitDiffEntry.Deletion;
import org.accula.api.code.git.GitDiffEntry.Modification;
import org.accula.api.code.git.GitDiffEntry.Renaming;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.git.Identifiable;
import org.accula.api.code.git.Snippet;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Anton Lamtev
 */
public final class GitCodeLoader implements CodeLoader {
    private static final String GITHUB_BASE_URL = "https://github.com/";
    private static final String GIT_EXTENSION = ".git";

    private final Git git;

    public GitCodeLoader(final Path root) {
        this.git = new Git(root, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 10));
    }

    @Override
    public Flux<FileEntity> loadFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        return withCommonGitRepo(snapshot)
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.getSha()))
                        .map(files -> files.stream().filter(file -> filter.test(file.getName())).collect(toList()))
                        .flatMap(files -> Mono
                                .fromFuture(repo.catFiles(files))
                                .map(filesContent -> files
                                        .stream()
                                        .map(file -> new FileEntity(snapshot, file.getName(), filesContent.get(file))))))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<FileEntity> loadSnippets(final CommitSnapshot snapshot, final List<SnippetMarker> markers) {
        return withCommonGitRepo(snapshot)
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.getSha()))
                        .map(files -> convertSnippets(files, markers))
                        .flatMap(snippets -> Mono
                                .fromFuture(repo.catFiles(snippets))
                                .map(filesContent -> snippets
                                        .stream()
                                        .map(snippet -> new FileEntity(
                                                snapshot, snippet.getFile().getName(),
                                                filesContent.get(snippet))))))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<DiffEntry> loadDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        return withCommonGitRepo(head)
                .flatMapMany(repo -> loadDiff(repo, base, head, filter, 0));
    }

    @Override
    public Flux<DiffEntry> loadRemoteDiff(final GithubRepo projectRepo,
                                          final CommitSnapshot base,
                                          final CommitSnapshot head,
                                          final FileFilter filter) {
        return withProjectGitRepo(projectRepo)
                .flatMap(repo -> addOrUpdateRemotes(repo, base, head))
                .flatMapMany(repo -> loadDiff(repo, base, head, filter, 1));
    }

    /// We name each common repo git folder like that: <owner-login>_<repo-name>
    private Mono<Repo> withCommonGitRepo(final CommitSnapshot snapshot) {
        final var snapshotRepo = snapshot.getRepo();
        final var repoGitDirectory = Path.of(snapshotRepo.getOwner().getLogin() + "_" + snapshotRepo.getName());
        final var repoUrl = repoGitUrl(snapshotRepo);
        return withGitRepo(repoGitDirectory, repoUrl);
    }

    private Mono<Repo> withProjectGitRepo(final GithubRepo projectRepo) {
        final var projectGitDirectory = Path.of(projectRepo.getName());
        final var projectRepoUrl = repoGitUrl(projectRepo);
        return withGitRepo(projectGitDirectory, projectRepoUrl);
    }

    private Mono<Repo> withGitRepo(final Path directory, final String url) {
        return Mono
                .fromFuture(git.repo(directory))
                .switchIfEmpty(Mono.fromFuture(git.clone(url, directory.toString())))
                .flatMap(repo -> Mono.fromFuture(repo.fetch()));
    }

    private Mono<Repo> addOrUpdateRemotes(final Repo repo, final CommitSnapshot base, final CommitSnapshot head) {
        final var baseRemote = base.getRepo().getOwner().getLogin();
        final var headRemote = head.getRepo().getOwner().getLogin();
        final var baseUrl = repoGitUrl(base.getRepo());
        final var headUrl = repoGitUrl(head.getRepo());
        return Mono
                .fromFuture(repo.remote())
                .flatMap(remotesPresent -> Mono
                        .zip(
                                addOrUpdateRemote(repo, baseUrl, baseRemote, remotesPresent),
                                addOrUpdateRemote(repo, headUrl, headRemote, remotesPresent),
                                (firstRepo, secondRepo) -> firstRepo
                        ));
    }

    private static Mono<Repo> addOrUpdateRemote(final Repo repo,
                                                final String remoteUrl,
                                                final String remote,
                                                final Set<String> remotesPresent) {
        return Mono.fromFuture(remotesPresent.contains(remote) ? repo.remoteUpdate(remote) : repo.remoteAdd(remoteUrl, remote));
    }

    private static Flux<DiffEntry> loadDiff(final Repo repo,
                                            final CommitSnapshot base,
                                            final CommitSnapshot head,
                                            final FileFilter filter,
                                            final int findRenamesMinSimilarityIndex) {
        return Mono
                .fromFuture(repo.diff(base.getSha(), head.getSha(), findRenamesMinSimilarityIndex))
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

    private static Function<
            Mono<Map<Identifiable, String>>,
            Mono<Stream<DiffEntry>>
            > convertDiffEntries(final List<GitDiffEntry> diffEntries, final CommitSnapshot base, final CommitSnapshot head) {
        return filesMono -> filesMono
                .map(files -> diffEntries
                        .stream()
                        .map(diffEntry -> {
                            if (diffEntry instanceof Addition) {
                                final var addition = (Addition) diffEntry;
                                return DiffEntry.of(
                                        FileEntity.absent(base),
                                        new FileEntity(head, addition.getHead().getName(), files.get(addition.getHead()))
                                );
                            }
                            if (diffEntry instanceof Deletion) {
                                final var deletion = (Deletion) diffEntry;
                                return DiffEntry.of(
                                        new FileEntity(base, deletion.getBase().getName(), files.get(deletion.getBase())),
                                        FileEntity.absent(head)
                                );
                            }
                            if (diffEntry instanceof Modification) {
                                final var modification = (Modification) diffEntry;
                                return DiffEntry.of(
                                        new FileEntity(base, modification.getBase().getName(), files.get(modification.getBase())),
                                        new FileEntity(head, modification.getHead().getName(), files.get(modification.getHead()))
                                );
                            }
                            if (diffEntry instanceof Renaming) {
                                final var renaming = (Renaming) diffEntry;
                                return new DiffEntry(
                                        new FileEntity(base, renaming.getBase().getName(), files.get(renaming.getBase())),
                                        new FileEntity(head, renaming.getHead().getName(), files.get(renaming.getHead())),
                                        renaming.getSimilarityIndex()
                                );
                            }

                            return null;
                        }));
    }

    private static List<Snippet> convertSnippets(final List<GitFile> files, final List<SnippetMarker> markers) {
        final var nameToFileMap = files
                .stream()
                .collect(toMap(GitFile::getName, file -> file));
        return markers
                .stream()
                .map(marker -> {
                    final var file = nameToFileMap.get(marker.getFilename());
                    if (file == null) {
                        return null;
                    }
                    return Snippet.of(file, marker.getFromLine(), marker.getToLine());
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static String repoGitUrl(final GithubRepo repo) {
        return GITHUB_BASE_URL + repo.getOwner().getLogin() + "/" + repo.getName() + GIT_EXTENSION;
    }
}
