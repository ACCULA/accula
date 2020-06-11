package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: scan root on start and fill cache with already cloned repos // looks like we don't need this cache
 * TODO: use 1 repo per sha. synchronize parallel access to repos.
 *
 * @author Vadim Dyachkov
 */
@RequiredArgsConstructor
public class RepositoryManager implements RepositoryProvider {
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();
    private final Map<RepoRef, RemoteCall> remoteCalls = new ConcurrentHashMap<>();
    private final File root;

    @Value
    private static class RepoRef {
        String owner;
        String repo;

        public String getUrl() {
            return GITHUB_BASE_URL + toString();
        }

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }

    //FIXME: this is a temporary solution which guarantees no race conditions
    @RequiredArgsConstructor
    @SuppressWarnings("PMD.RedundantFieldInitializer")
    private static class RemoteCall {
        final Scheduler scheduler;
        boolean isCloned = false;
    }

    @Override
    public Mono<Repository> getRepository(final String owner, final String repo) {
        final RepoRef ref = new RepoRef(owner, repo);
        final File directory = getDirectory(ref);
        final var remoteCall = remoteCalls.computeIfAbsent(ref, r -> new RemoteCall(Schedulers.newSingle(r.toString())));
        return Mono
                .justOrEmpty(cache.get(ref))
                .switchIfEmpty(openRepository(directory))
                .switchIfEmpty(cloneRepository(ref, directory, remoteCall))
                .flatMap(repository -> doFetch(repository, remoteCall))
                .doOnSuccess(rep -> cache.put(ref, rep));
    }

    @SneakyThrows
    private Repository getFileRepository(final File dir) {
        return new FileRepository(new File(dir, ".git"));
    }

    private File getDirectory(final RepoRef ref) {
        return new File(new File(root, ref.owner), ref.repo);
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

    private Mono<Repository> doFetch(final Repository repository, final RemoteCall remoteCall) {
        return Mono
                .fromCallable(() -> {
                    Git.wrap(repository).fetch().call();
                    return repository;
                })
                .subscribeOn(remoteCall.scheduler);
    }
}
