package org.accula.api.code;

import org.accula.api.code.git.DiffEntry;
import org.accula.api.code.git.DiffEntry.Addition;
import org.accula.api.code.git.DiffEntry.Deletion;
import org.accula.api.code.git.DiffEntry.Modification;
import org.accula.api.code.git.DiffEntry.Renaming;
import org.accula.api.code.git.Git;
import org.accula.api.code.git.Snippet;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;
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
        this.git = new Git(root, Executors.newFixedThreadPool(10));
    }

    @Override
    public Flux<FileEntity> loadFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        final var repoGitDirectory = commonRepoGitDirectory(snapshot);
        final var repoUrl = repoGitUrl(snapshot.getRepo());
        return Mono
                .fromFuture(git.repo(repoGitDirectory))
                .switchIfEmpty(Mono.fromFuture(git.clone(repoUrl, repoGitDirectory.toString())))
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.getSha()))
                        .map(files -> files.stream().filter(file -> filter.test(file.name)).collect(toList()))
                        .flatMap(files -> Mono
                                .fromFuture(repo.catFiles(files))
                                .map(filesContent -> files
                                        .stream()
                                        .map(file -> new FileEntity(snapshot, file.name, filesContent.get(file.id))))))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<FileEntity> loadSnippets(final CommitSnapshot snapshot, final List<SnippetMarker> markers) {
        final var markerMap = markers.stream().collect(toMap(SnippetMarker::getFilename, marker -> marker));
        final var repoGitDirectory = commonRepoGitDirectory(snapshot);
        final var repoUrl = repoGitUrl(snapshot.getRepo());
        return Mono
                .fromFuture(git.repo(repoGitDirectory))
                .switchIfEmpty(Mono.fromFuture(git.clone(repoUrl, repoGitDirectory.toString())))
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.getSha()))
                        .map(files -> files
                                .stream()
                                .map(file -> {
                                    final var marker = markerMap.get(file.name);
                                    if (marker == null) {
                                        return null;
                                    }
                                    return Snippet.of(file, marker.getFromLine(), marker.getToLine());
                                })
                                .filter(Objects::nonNull)
                                .collect(toList()))
                        .flatMap(snippets -> Mono
                                .fromFuture(repo.catFiles(snippets))
                                .map(filesContent -> snippets
                                        .stream()
                                        .map(snippet -> new FileEntity(snapshot, snippet.file.name, filesContent.get(snippet.file.id))))))
                .flatMapMany(Flux::fromStream);
    }

    @Override
    public Flux<org.accula.api.code.DiffEntry> loadDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        final var repoGitDirectory = commonRepoGitDirectory(head);
        final var repoUrl = repoGitUrl(head.getRepo());
        return Mono
                .fromFuture(git.repo(repoGitDirectory))
                .switchIfEmpty(Mono.fromFuture(git.clone(repoUrl, repoGitDirectory.toString())))
                .flatMapMany(repo -> Mono
                        .fromFuture(repo.diff(base.getSha(), head.getSha(), 0))
                        .map(diffEntries -> diffEntries
                                .stream()
                                .filter(entry -> entry.passes(filter))
                                .collect(toList()))
                        .flatMapMany(diffEntries -> Mono
                                .fromFuture(repo
                                        .catFiles(diffEntries
                                                .stream()
                                                .flatMap(DiffEntry::objectIds)
                                                .collect(toList())))
                                .transform(convertDiffEntries(diffEntries, base, head))
                                .flatMapMany(Flux::fromStream)));
    }

    @Override
    public Flux<org.accula.api.code.DiffEntry> loadRemoteDiff(final GithubRepo projectRepo,
                                                              final CommitSnapshot base,
                                                              final CommitSnapshot head,
                                                              final FileFilter filter) {
        final var projectGitDirectory = Paths.get(projectRepo.getName());
        final var projectRepoUrl = repoGitUrl(projectRepo);

        final var baseRemote = base.getRepo().getOwner().getLogin();
        final var headRemote = head.getRepo().getOwner().getLogin();

        final var baseRef = base.getSha();
        final var headRef = head.getSha();

        final var baseUrl = repoGitUrl(base.getRepo());
        final var headUrl = repoGitUrl(head.getRepo());

        return Mono
                .fromFuture(git.repo(projectGitDirectory))
                .switchIfEmpty(Mono.fromFuture(git.clone(projectRepoUrl, projectGitDirectory.toString())))
                .flatMapMany(repo -> Mono
                        .fromFuture(repo.remoteAdd(baseUrl, baseRemote))
                        .zipWith(Mono.fromFuture(repo.remoteAdd(headUrl, headRemote)), (baseSuccess, headSuccess) -> baseSuccess && headSuccess)
                        .filter(isEqual(TRUE))
                        .flatMapMany(success -> Mono
                                .fromFuture(repo.diff(baseRef, headRef, 1))
                                .map(diffEntries -> diffEntries
                                        .stream()
                                        .filter(entry -> entry.passes(filter))
                                        .collect(toList()))
                                .flatMapMany(diffEntries -> Mono
                                        .fromFuture(repo
                                                .catFiles(diffEntries
                                                        .stream()
                                                        .flatMap(DiffEntry::objectIds)
                                                        .collect(toList())))
                                        .transform(convertDiffEntries(diffEntries, base, head))
                                        .flatMapMany(Flux::fromStream))));
    }

    private static Function<
            Mono<Map<String, String>>,
            Mono<Stream<org.accula.api.code.DiffEntry>>> convertDiffEntries(final List<DiffEntry> diffEntries,
                                                                            final CommitSnapshot base,
                                                                            final CommitSnapshot head) {
        return filesMono -> filesMono
                .map(files -> diffEntries
                        .stream()
                        .map(diffEntry -> {
                            if (diffEntry instanceof Addition) {
                                final var addition = (Addition) diffEntry;
                                return org.accula.api.code.DiffEntry.of(FileEntity.absent(base), new FileEntity(head, addition.head.name, files.get(addition.head.id)));
                            }
                            if (diffEntry instanceof Deletion) {
                                final var deletion = (Deletion) diffEntry;
                                return org.accula.api.code.DiffEntry.of(new FileEntity(base, deletion.base.name, files.get(deletion.base.id)), FileEntity.absent(head));
                            }
                            if (diffEntry instanceof Modification) {
                                final var modification = (Modification) diffEntry;
                                return org.accula.api.code.DiffEntry.of(
                                        new FileEntity(base, modification.base.name, files.get(modification.base.id)),
                                        new FileEntity(head, modification.head.name, files.get(modification.head.id)));
                            }
                            if (diffEntry instanceof Renaming) {
                                final var renaming = (Renaming) diffEntry;
                                return new org.accula.api.code.DiffEntry(
                                        new FileEntity(base, renaming.base.name, files.get(renaming.base.id)),
                                        new FileEntity(head, renaming.head.name, files.get(renaming.head.id)),
                                        renaming.similarityIndex);
                            }

                            return null;
                        }));
    }

    private static Path commonRepoGitDirectory(final CommitSnapshot snapshot) {
        return Paths.get(snapshot.getRepo().getOwner().getLogin() + "_" + snapshot.getRepo().getName());
    }

    private static String repoGitUrl(final GithubRepo repo) {
        return GITHUB_BASE_URL + repo.getOwner().getLogin() + "/" + repo.getName() + GIT_EXTENSION;
    }
}
