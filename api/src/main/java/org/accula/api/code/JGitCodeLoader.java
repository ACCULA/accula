package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.util.FileContentCutter;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.util.AcculaSchedulers;
import org.accula.api.util.Sync;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public class JGitCodeLoader implements CodeLoader {
    public static final Exception REPO_NOT_FOUND = new Exception();
    public static final Exception FILE_NOT_FOUND = new Exception();
    public static final Exception CUT_ERROR = new Exception();
    public static final Exception RANGE_ERROR = new Exception();
    private static final String DELETED_FILE = "/dev/null";
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Scheduler scheduler = AcculaSchedulers.newBoundedElastic(getClass().getSimpleName());
    private final Scheduler remoteCallsScheduler = AcculaSchedulers.newBoundedElastic(getClass().getSimpleName() + "-remote");
    private final Map<RepoRef, Repository> clonedRepos = new ConcurrentHashMap<>();
    private final Map<RepoRef, Sync> accessSynchronizers = new ConcurrentHashMap<>();
    private final File root;

    @Value
    private static class RepoRef {
        String owner;
        String repo;
        String sha;

        static RepoRef from(final CommitSnapshot snapshot) {
            final var repo = snapshot.getRepo();
            return new RepoRef(repo.getOwner().getLogin(), repo.getName(), snapshot.getSha());
        }

        String getUrl() {
            return GITHUB_BASE_URL + toString();
        }

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }

    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        final var ref = RepoRef.from(snapshot);
        final var dir = getDirectory(ref);
        final var accessSync = accessSynchronizers.computeIfAbsent(ref, Sync::create);
        return getRepo(ref, dir, accessSync)
                .publishOn(scheduler)
                .flatMapMany(repo -> getObjectLoaders(repo, accessSync, snapshot.getSha()))
                .filter(filenameAndLoader -> filter.test(filenameAndLoader.getT1()))
                .map(filenameAndLoader -> new FileEntity(snapshot, filenameAndLoader.getT1(), getFileContent(filenameAndLoader.getT2())))
                .onErrorResume(e -> e instanceof MissingObjectException, e -> {
                    log.info("Most probably branch with the commit {} has been deleted: {}", snapshot.toString(), e);
                    return Flux.empty();
                });
    }

    @Override
    public Mono<FileEntity> getFileSnippet(final CommitSnapshot snapshot, final String filename, final int fromLine, final int toLine) {
        if (fromLine > toLine) {
            return Mono.error(RANGE_ERROR);
        }
        final var ref = RepoRef.from(snapshot);
        final var dir = getDirectory(ref);
        final var accessSync = accessSynchronizers.computeIfAbsent(ref, Sync::create);
        return getRepo(ref, dir, accessSync)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .publishOn(scheduler)
                .flatMap(repo -> Mono.justOrEmpty(getObjectLoader(repo, accessSync, snapshot.getSha(), filename)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(loader -> new FileEntity(snapshot, filename, FileContentCutter.cutFileContent(loader, fromLine, toLine)))
                .doOnSuccess(f -> log.debug("Loaded file entity: {}/{}", f.getCommitSnapshot(), f.getName()))
                .switchIfEmpty(Mono.error(CUT_ERROR));
    }

    @Override
    public Flux<DiffEntry> getDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        final var ref = RepoRef.from(head);
        final var dir = getDirectory(ref);
        final var accessSync = accessSynchronizers.computeIfAbsent(ref, Sync::create);
        return getRepo(ref, dir, accessSync)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .publishOn(scheduler)
                .flatMapMany(repo -> getDiffEntries(repo, accessSync, base, head))
                .filter(diff -> passesFilter(diff, filter, base, head))
                .parallel()
                .runOn(scheduler)
                .flatMap(diff -> Mono.zip(getFileNullable(base, diff.getOldPath()), getFileNullable(head, diff.getNewPath())))
                .map(TupleUtils.function(DiffEntry::of))
                .sequential()
                .onErrorResume(MissingObjectException.class, e -> Flux.empty());
    }

    @Override
    public Flux<DiffEntry> getRemoteDiff(final GithubRepo projectRepo, final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        final var ref = RepoRef.from(base);
        final var dir = getDirectory(ref);
        final var accessSync = accessSynchronizers.computeIfAbsent(ref, Sync::create);
        return getRepo(ref, dir, accessSync)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMap(repo -> addAndFetchRemote(repo, accessSync, head))
                .flatMapMany(repo -> getDiffEntries(repo, accessSync, base, head))
                .filter(diff -> passesFilter(diff, filter, base, head))
                .parallel()
                .runOn(scheduler)
                .flatMap(diff -> Mono.zip(getFileNullable(base, diff.getOldPath()), getFileNullable(head, diff.getNewPath())))
                .map(TupleUtils.function(DiffEntry::of))
                .sequential()
                .onErrorResume(MissingObjectException.class, e -> Flux.empty());
    }

    private static boolean passesFilter(final org.eclipse.jgit.diff.DiffEntry entry,
                                        final FileFilter filter,
                                        final CommitSnapshot base,
                                        final CommitSnapshot head) {
        final var result = (entry.getOldPath().equals(DELETED_FILE) || filter.test(entry.getOldPath()))
                           && (entry.getNewPath().equals(DELETED_FILE) || filter.test(entry.getNewPath()));
        if (!result) {
            log.debug("Skipping diff: {} for base {} and head {}", entry, base, head);
        }
        return result;
    }

    @SneakyThrows
    private Flux<org.eclipse.jgit.diff.DiffEntry> getDiffEntries(final Repository repo,
                                                                 final Sync accessSync,
                                                                 final CommitSnapshot base,
                                                                 final CommitSnapshot head) {
        return Flux.fromIterable(accessSync.withReadLock(() -> Git
                .wrap(repo)
                .diff()
                .setOldTree(getTreeIterator(repo, accessSync, base.getSha()))
                .setNewTree(getTreeIterator(repo, accessSync, head.getSha()))
                .call()));
    }

    @SneakyThrows
    private AbstractTreeIterator getTreeIterator(final Repository repository, final Sync accessSync, final String sha) {
        return accessSync.withReadLock(() -> {
            final ObjectReader reader = repository.newObjectReader();
            final RevWalk revWalk = new RevWalk(reader);
            final ObjectId commitId = repository.resolve(sha);
            final RevCommit commit = revWalk.parseCommit(commitId);
            final RevTree revTree = commit.getTree();
            final CanonicalTreeParser tree = new CanonicalTreeParser();
            tree.reset(reader, revTree.getId());
            return tree;
        });
    }

    private Mono<FileEntity> getFileNullable(final CommitSnapshot snapshot, final String filename) {
        if (DELETED_FILE.equals(filename)) {
            return Mono.just(FileEntity.absent(snapshot));
        }
        final var ref = RepoRef.from(snapshot);
        final var dir = getDirectory(ref);
        final var accessSync = accessSynchronizers.computeIfAbsent(ref, Sync::create);
        return getRepo(ref, dir, accessSync)
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .publishOn(scheduler)
                .flatMap(repo -> Mono.justOrEmpty(getObjectLoader(repo, accessSync, snapshot.getSha(), filename)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(loader -> new FileEntity(snapshot, filename, getFileContent(loader)))
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .doOnSuccess(f -> log.info("Finished {}/{}", f.getCommitSnapshot(), f.getName()));
    }

    @SneakyThrows
    @Nullable
    private ObjectLoader getObjectLoader(final Repository repository,
                                         final Sync accessSync,
                                         final String sha,
                                         final String file) {
        return accessSync.withReadLockNullable(() -> {
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
        });
    }

    private Flux<Tuple2<String, ObjectLoader>> getObjectLoaders(final Repository repo,
                                                                final Sync accessSync,
                                                                final String sha) {
        return Flux.fromIterable(accessSync.withReadLock(() -> {
            final ObjectReader reader = repo.newObjectReader();
            final RevWalk revWalk = new RevWalk(reader);
            final ObjectId commitId = repo.resolve(sha);
            final RevCommit commit = revWalk.parseCommit(commitId);
            final RevTree revTree = commit.getTree();

            final List<Tuple2<String, ObjectLoader>> result = new ArrayList<>();
            final TreeWalk treeWalk = new TreeWalk(repo);
            final int treeId = treeWalk.addTree(revTree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                final ObjectId objectId = treeWalk.getObjectId(treeId);
                result.add(Tuples.of(treeWalk.getPathString(), reader.open(objectId)));
            }
            return result;
        }));
    }

    private String getFileContent(final ObjectLoader loader) {
        if (loader.isLarge()) {
            return FileContentCutter.cutFileContent(loader, 1, Integer.MAX_VALUE);
        }
        return new String(loader.getBytes(), StandardCharsets.UTF_8);
    }

    private Mono<Repository> addAndFetchRemote(final Repository originRepo,
                                               final Sync accessSync,
                                               final CommitSnapshot remote) {
        return Mono
                .fromSupplier(() -> accessSync
                        .withWriteLockNullable(() -> {
                            final var remoteRef = RepoRef.from(remote);
                            final var remoteName = remoteRef.owner + "_" + remoteRef.repo;
                            final var git = Git.wrap(originRepo);
                            git.remoteAdd().setName(remoteName).setUri(new URIish(remoteRef.getUrl())).call();
                            git.fetch().setRemote(remoteName).setForceUpdate(true).call();
                            return originRepo;
                        }))
                .subscribeOn(remoteCallsScheduler);
    }

    private Mono<Repository> getRepo(final RepoRef ref, final File dir, final Sync accessSync) {
        return openRepo(ref, dir, accessSync)
                .switchIfEmpty(cloneRepo(ref, dir, accessSync));
    }

    private Mono<Repository> openRepo(final RepoRef ref, final File directory, final Sync accessSync) {
        return Mono
                .fromSupplier(() -> accessSync
                        .withReadLockNullable(() -> {
                            var repo = clonedRepos.get(ref);
                            if (repo != null) {
                                return repo;
                            }
                            if (directory.exists()) {
                                repo = new FileRepository(new File(directory, ".git"));
                                clonedRepos.put(ref, repo);
                                return repo;
                            }
                            return null;
                        }))
                .onErrorResume(e -> {
                    log.error("Error occurred: ", e);
                    return Mono.empty();
                });
    }

    private Mono<Repository> cloneRepo(final RepoRef ref, final File dir, final Sync accessSync) {
        return Mono
                .fromSupplier(() -> accessSync
                        .withWriteLockNullable(() -> {
                            var repo = clonedRepos.get(ref);
                            if (repo != null) {
                                return repo;
                            }
                            repo = Git.cloneRepository()
                                    .setDirectory(dir)
                                    .setURI(ref.getUrl())
                                    .call()
                                    .getRepository();
                            clonedRepos.put(ref, repo);
                            return repo;
                        }))
                .subscribeOn(remoteCallsScheduler);
    }

    private File getDirectory(final RepoRef ref) {
        return new File(root, ref.getSha());
    }
}
