package org.accula.api.github.api;

import org.accula.api.github.model.GithubHook;
import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubRepo;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface GithubClient {
    Mono<Boolean> hasAdminPermission(String owner, String repo);

    Mono<GithubRepo> getRepo(String owner, String repo);

    Mono<GithubPull[]> getRepositoryPulls(String owner, String repo, GithubPull.State state);

    Mono<GithubPull> getRepositoryPull(String owner, String repo, Integer pullNumber);

    Mono<Void> createHook(String owner, String repo, GithubHook hook);

    interface LoginProvider {
        Mono<String> login();
    }

    interface AccessTokenProvider {
        Mono<String> accessToken();
    }
}
