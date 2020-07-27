package org.accula.api.code;

import org.accula.api.code.git.File;
import org.accula.api.code.git.Git;
import org.accula.api.code.git.GitDiffEntry;
import org.accula.api.code.git.GitDiffEntry.Addition;
import org.accula.api.code.git.GitDiffEntry.Deletion;
import org.accula.api.code.git.GitDiffEntry.Modification;
import org.accula.api.code.git.GitDiffEntry.Renaming;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
public final class GitCodeLoader implements CodeLoader {
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Git git;

    public GitCodeLoader(final Path root) {
        this.git = new Git(root, Executors.newFixedThreadPool(10));
    }

    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        final var directory = Paths.get(snapshot.getRepo().getOwner().getLogin() + "_" + snapshot.getRepo().getName());
        final var url = GITHUB_BASE_URL + snapshot.getRepo().getOwner().getLogin() + "/" + snapshot.getRepo().getName() + ".git";
        return Mono
                .fromFuture(git.repo(directory))
                .switchIfEmpty(Mono.fromFuture(git.clone(url, directory.toString())))
                .flatMap(repo -> Mono
                        .fromFuture(repo.lsTree(snapshot.getSha()))
                        .map(files -> files.stream().filter(file -> filter.test(file.name)).collect(toList()))
                        .flatMap(files -> Mono
                                .fromFuture(repo.catFiles(files.stream().map(File::getObjectId).collect(toList())))
                                .map(filesContent -> files
                                        .stream()
                                        .map(file -> new FileEntity(snapshot, file.name, filesContent.get(file.objectId))))))
                .flatMapMany(Flux::fromStream);
    }

    @Deprecated
    @Override
    public Mono<FileEntity> getFileSnippet(final CommitSnapshot snapshot, final String filename, final int fromLine, final int toLine) {
        return Mono.empty();
    }

    @Override
    public Flux<FileEntity> getFileSnippets(List<SnippetMarker> markers) {
        return Flux.empty();
    }

    @Override
    public Flux<DiffEntry> getDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        final var directory = Paths.get(head.getRepo().getOwner().getLogin() + "_" + head.getRepo().getName());
        final var url = GITHUB_BASE_URL + head.getRepo().getOwner().getLogin() + "/" + head.getRepo().getName() + ".git";
        return Mono
                .fromFuture(git.repo(directory))
                .switchIfEmpty(Mono.fromFuture(git.clone(url, directory.toString())))
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
                                                .flatMap(GitDiffEntry::objectIds)
                                                .collect(toList())))
                                .map(files -> diffEntries
                                        .stream()
                                        .map(diffEntry -> {
                                            if (diffEntry instanceof Addition) {
                                                final var addition = (Addition) diffEntry;
                                                return DiffEntry.of(FileEntity.absent(base), new FileEntity(head, addition.head.name, files.get(addition.head.objectId)));
                                            }
                                            if (diffEntry instanceof Deletion) {
                                                final var deletion = (Deletion) diffEntry;
                                                return DiffEntry.of(new FileEntity(base, deletion.base.name, files.get(deletion.base.objectId)), FileEntity.absent(head));
                                            }
                                            if (diffEntry instanceof Modification) {
                                                final var modification = (Modification) diffEntry;
                                                return DiffEntry.of(
                                                        new FileEntity(base, modification.base.name, files.get(modification.base.objectId)),
                                                        new FileEntity(head, modification.head.name, files.get(modification.head.objectId)));
                                            }
                                            if (diffEntry instanceof Renaming) {
                                                final var renaming = (Renaming) diffEntry;
                                                return new DiffEntry(
                                                        new FileEntity(base, renaming.base.name, files.get(renaming.base.objectId)),
                                                        new FileEntity(head, renaming.head.name, files.get(renaming.head.objectId)),
                                                        renaming.similarityIndex);
                                            }

                                            return null;
                                        }))
                                .flatMapMany(Flux::fromStream)))
                .map(it -> it);
    }

    @Override
    public Flux<DiffEntry> getRemoteDiff(final GithubRepo projectRepo,
                                         final CommitSnapshot origin,
                                         final CommitSnapshot remote,
                                         final FileFilter filter) {
        final var directory = Paths.get(projectRepo.getName());
        final var url = GITHUB_BASE_URL + projectRepo.getOwner().getLogin() + "/" + projectRepo.getName() + ".git";

        Mono
                .fromFuture(git.repo(directory))
                .switchIfEmpty(Mono.fromFuture(git.clone(url, directory.toString())))
                .flatMap(repo -> Mono
                        .fromFuture(repo.remote())
                        .map(remotes -> remotes));
        return Flux.empty();
    }
}
