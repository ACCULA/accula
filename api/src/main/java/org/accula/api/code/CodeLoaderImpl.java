package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.db.model.CommitSnapshot;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CodeLoaderImpl implements CodeLoader {
    public static final Exception REPO_NOT_FOUND = new Exception();
    public static final Exception FILE_NOT_FOUND = new Exception();
    public static final Exception CUT_ERROR = new Exception();
    public static final Exception RANGE_ERROR = new Exception();
    private static final String DELETED_FILE = "/dev/null";

    private final Scheduler scheduler = Schedulers.boundedElastic();

    private final RepositoryProvider repositoryProvider;

    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot) {
        return getFiles(snapshot, FileFilter.ALL);
    }

    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        return getRepository(snapshot)
                .publishOn(scheduler)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMapMany(repo -> Flux.fromIterable(getObjectLoaders(repo, snapshot.getCommitSha())))
                .filter(filenameAndLoader -> filter.test(filenameAndLoader.getT1()))
                .map(filenameAndLoader -> new FileEntity(snapshot, filenameAndLoader.getT1(), getFileContent(filenameAndLoader.getT2())))
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .onErrorResume(e -> e instanceof MissingObjectException, e -> {
                    log.info("Most probably branch with the commit {} has been deleted: {}", snapshot.toString(), e);
                    return Flux.empty();
                });
    }

    @Override
    public Mono<FileEntity> getFile(final CommitSnapshot snapshot, final String filename) {
        return getRepository(snapshot)
                .publishOn(scheduler)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMap(repo -> Mono.justOrEmpty(getObjectLoader(repo, snapshot.getCommitSha(), filename)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(loader -> new FileEntity(snapshot, filename, getFileContent(loader)))
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .doOnSuccess(f -> log.info("Finished {}/{}", f.getCommitSnapshot(), f.getName()));
    }

    @Override
    public Mono<FileEntity> getFileSnippet(final CommitSnapshot snapshot, final String filename, final int fromLine, final int toLine) {
        if (fromLine > toLine) {
            return Mono.error(RANGE_ERROR);
        }
        return getRepository(snapshot)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMap(repo -> Mono.justOrEmpty(getObjectLoader(repo, snapshot.getCommitSha(), filename)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(loader -> new FileEntity(snapshot, filename, cutFileContent(loader, fromLine, toLine)))
                .doOnSuccess(f -> log.debug("Loaded file entity: {}/{}", f.getCommitSnapshot(), f.getName()))
                .switchIfEmpty(Mono.error(CUT_ERROR));
    }

    public Flux<Tuple2<FileEntity, FileEntity>> getDiff(final CommitSnapshot base, final CommitSnapshot head) {
        return getDiff(base, head, FileFilter.ALL);
    }

    @Override
    public Flux<Tuple2<FileEntity, FileEntity>> getDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        final Mono<AbstractTreeIterator> baseTree = getRepository(base)
                .map(repo -> getTreeIterator(repo, base.getCommitSha()))
                .doOnError(e -> log.error("Cannot get tree iterator for base {}: {}", base, e.getMessage()));

        final Mono<Repository> headRepo = getRepository(head).cache();
        final Mono<AbstractTreeIterator> headTree = headRepo
                .map(repo -> getTreeIterator(repo, head.getCommitSha()))
                .doOnError(e -> log.error("Cannot get tree iterator for head {}: {}", head, e.getMessage()));

        return Mono.zip(headRepo, baseTree, headTree)
                .flatMapMany(repoBaseHead -> getDiffEntries(repoBaseHead.getT1(), repoBaseHead.getT2(), repoBaseHead.getT3()))
                .filter(diff -> {
                    final var result = (diff.getOldPath().equals(DELETED_FILE) || filter.test(diff.getOldPath()))
                            && (diff.getNewPath().equals(DELETED_FILE) || filter.test(diff.getNewPath()));
                    if (!result) {
                        log.debug("Skipping diff: {} for base {} and head {}", diff, base, head);
                    }
                    return result;
                })
                .parallel()
                .flatMap(diff -> Mono.zip(getFileNullable(base, diff.getOldPath()), getFileNullable(head, diff.getNewPath())))
                .sequential()
                .onErrorResume(MissingObjectException.class, e -> Flux.empty());
    }

    private Mono<Repository> getRepository(final CommitSnapshot snapshot) {
        return repositoryProvider.getRepository(snapshot.getRepo().getOwner().getLogin(), snapshot.getRepo().getName());
    }

    @SneakyThrows
    private Flux<DiffEntry> getDiffEntries(final Repository repo, final AbstractTreeIterator base, final AbstractTreeIterator head) {
        return Flux.fromIterable(Git
                .wrap(repo)
                .diff()
                .setOldTree(base)
                .setNewTree(head)
                .call());
    }

    @SneakyThrows
    private AbstractTreeIterator getTreeIterator(final Repository repository, final String sha) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree revTree = commit.getTree();
        final CanonicalTreeParser tree = new CanonicalTreeParser();
        tree.reset(reader, revTree.getId());
        return tree;
    }

    private Mono<FileEntity> getFileNullable(final CommitSnapshot snapshot, final String filename) {
        if (DELETED_FILE.equals(filename)) {
            return Mono.just(new FileEntity(snapshot, null, null));
        }
        return getFile(snapshot, filename);
    }

    @SneakyThrows
    @Nullable
    private ObjectLoader getObjectLoader(final Repository repository, final String sha, final String file) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree revTree = commit.getTree();
        final TreeWalk treeWalk = TreeWalk.forPath(reader, file, revTree);
        if (treeWalk == null) {
            log.error("Cannot find file {} in repo {}, sha={}", file, repository.getWorkTree(), sha);
            return null;
        }
        return reader.open(treeWalk.getObjectId(0));
    }

    @SneakyThrows
    private List<Tuple2<String, ObjectLoader>> getObjectLoaders(final Repository repository, final String sha) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree revTree = commit.getTree();

        final List<Tuple2<String, ObjectLoader>> result = new ArrayList<>();
        final TreeWalk treeWalk = new TreeWalk(repository);
        final int treeId = treeWalk.addTree(revTree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            final ObjectId objectId = treeWalk.getObjectId(treeId);
            result.add(Tuples.of(treeWalk.getPathString(), reader.open(objectId)));
        }
        return result;
    }

    private String getFileContent(final ObjectLoader loader) {
        if (loader.isLarge()) {
            return cutFileContent(loader, 1, Integer.MAX_VALUE);
        }
        return new String(loader.getBytes(), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    private String cutFileContent(final ObjectLoader loader, final int fromLine, final int toLine) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try (InputStream is = loader.openStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            int skip = fromLine - 1;
            int take = toLine - skip;
            String line = br.readLine();
            while (line != null) {
                if (skip > 0) {
                    skip--;
                    line = br.readLine();
                    continue;
                }
                joiner.add(line);
                take--;
                if (take == 0) {
                    break;
                }
                line = br.readLine();
            }
        }
        return joiner.toString();
    }
}
