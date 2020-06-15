package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * TODO: scan root on start and fill cache with already cloned repos // looks like we don't need this cache
 * TODO: use 1 repo per sha. synchronize parallel access to repos.
 *
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public class RepositoryManager implements RepositoryProvider, RepositoryUpdater {
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();
    private final Map<RepoRef, RemoteCall> remoteCalls = new ConcurrentHashMap<>();
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

    //FIXME: this is a temporary solution which guarantees no race condition will happen
    @RequiredArgsConstructor
    @SuppressWarnings("PMD.RedundantFieldInitializer")
    private static class RemoteCall {
        final Scheduler scheduler;
        boolean isCloned = false;
    }

    @Override
    public Mono<Repository> getRepository(final CommitSnapshot snapshot) {
        final var ref = RepoRef.from(snapshot);
        final File directory = getDirectory(ref);
        final var remoteCall = remoteCalls.computeIfAbsent(ref, r -> new RemoteCall(Schedulers.newSingle(r.toString())));
        return Mono
                .justOrEmpty(cache.get(ref))
                .switchIfEmpty(openRepository(directory))
                .switchIfEmpty(cloneRepository(ref, directory, remoteCall))
                .doOnSuccess(rep -> cache.put(ref, rep));
    }

    @Override
    public Mono<Repository> addAndFetchRemote(final CommitSnapshot origin, final CommitSnapshot remote) {
        final var originRef = RepoRef.from(origin);
        final var remoteCall = remoteCalls.get(originRef);
        if (remoteCall == null) {
            return Mono.empty();
        }
        final File directory = getDirectory(originRef);

        return Mono.just(directory.exists())
                .filter(Predicate.isEqual(true))
                .map(exists -> getFileRepository(directory))
                .switchIfEmpty(cloneRepository(originRef, directory, remoteCall))
                .flatMap(repository -> Mono
                        .fromCallable(() -> {
                            final var git = Git.wrap(repository);
                            final var remoteRef = RepoRef.from(remote);
                            final var remoteName = remoteRef.owner + "_" + remoteRef.repo;
                            git.remoteAdd().setName(remoteName).setUri(new URIish(remoteRef.getUrl())).call();
                            git.fetch().setRemote(remoteName).setForceUpdate(true).call();
                            return repository;
                        })
                        .subscribeOn(remoteCall.scheduler));
    }

    @SneakyThrows
    private Repository getFileRepository(final File dir) {
        return new FileRepository(new File(dir, ".git"));
    }

    private File getDirectory(final RepoRef ref) {
        return new File(root, Integer.toString(ref.hashCode()));
    }

    private Mono<Repository> openRepository(final File directory) {
        return Mono
                .just(directory)
                .filter(File::exists)
                .map(this::getFileRepository)
                .onErrorResume(Exception.class, t -> Mono.empty());
    }

    private Mono<Repository> cloneRepository(final RepoRef ref, final File directory, final RemoteCall remoteCall) {
        return Mono
                .fromCallable(() -> {
                    if (remoteCall.isCloned) {
                        return getFileRepository(directory);
                    }
                    return Git.cloneRepository()
                            .setDirectory(directory)
                            .setURI(ref.getUrl())
                            .call()
                            .getRepository();
                })
                .doOnSuccess(repository -> remoteCall.isCloned = true)
                .subscribeOn(remoteCall.scheduler);
    }
}
