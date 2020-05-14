package org.accula.api.github.api;

import org.accula.api.github.model.Pull;
import org.accula.api.github.model.Repo;
import reactor.core.publisher.Mono;

public interface GithubClient {
    Mono<Boolean> hasAdminPermission(final String owner, final String repo);

    Mono<Repo> getRepo(final String owner, final String repo);

    Mono<Pull[]> getRepositoryOpenPulls(final String owner, final String repo);

    interface LoginProvider {
        Mono<String> login();
    }

    interface AccessTokenProvider {
        Mono<String> accessToken();
    }
}
