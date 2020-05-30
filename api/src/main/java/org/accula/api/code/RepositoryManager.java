package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: scan root on start and fill cache with already cloned repos
 *
 * @author Vadim Dyachkov
 */
@RequiredArgsConstructor
public class RepositoryManager implements RepositoryProvider {
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();

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

    @Override
    public Mono<Repository> getRepository(final String owner, final String repo) {
        final RepoRef ref = new RepoRef(owner, repo);
        final File directory = getDirectory(ref);
        return Mono
                .justOrEmpty(cache.getOrDefault(ref, null))
                .switchIfEmpty(openRepository(directory))
                .switchIfEmpty(cloneRepository(ref, directory))
                .map(this::doFetch)
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

    private Mono<Repository> cloneRepository(final RepoRef ref, final File directory) {
        return Mono
                .fromCallable(() -> Git
                        .cloneRepository()
                        .setDirectory(directory)
                        .setURI(ref.getUrl())
                        .call()
                        .getRepository());
    }

    @SneakyThrows
    private Repository doFetch(final Repository repository) {
        Git.wrap(repository).fetch().call();
        return repository;
    }
}
