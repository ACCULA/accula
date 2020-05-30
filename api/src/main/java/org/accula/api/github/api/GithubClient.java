package org.accula.api.github.api;

import org.accula.api.github.model.GithubHook;
import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubRepo;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface GithubClient {
    Mono<Boolean> hasAdminPermission(final String owner, final String repo);

    Mono<GithubRepo> getRepo(final String owner, final String repo);

    Mono<GithubPull[]> getRepositoryOpenPulls(final String owner, final String repo);

    Mono<GithubPull> getRepositoryPull(final String owner, final String repo, final Integer pullNumber);

    Mono<Void> createHook(final String owner, final String repo, final GithubHook hook);

    interface LoginProvider {
        Mono<String> login();
    }

    interface AccessTokenProvider {
        Mono<String> accessToken();
    }
}
