package org.accula.api.github.api;

import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiRepo;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface GithubClient {
    Mono<Boolean> hasAdminPermission(String owner, String repo);

    Mono<GithubApiRepo> getRepo(String owner, String repo);

    Mono<GithubApiPull[]> getRepositoryPulls(String owner, String repo, GithubApiPull.State state);

    Mono<GithubApiPull> getRepositoryPull(String owner, String repo, Integer pullNumber);

    Mono<Void> createHook(String owner, String repo, GithubApiHook hook);

    interface LoginProvider {
        Mono<String> login();
    }

    interface AccessTokenProvider {
        Mono<String> accessToken();
    }
}
