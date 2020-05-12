package org.accula.api.github.api;

import org.accula.api.github.model.Permission;
import org.accula.api.github.model.Pull;
import org.accula.api.github.model.Repo;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.lang.Boolean.FALSE;
import static org.accula.api.github.model.Permission.PermissionEnum.ADMIN;
import static org.springframework.http.HttpStatus.OK;

/**
 * @author Anton Lamtev
 * @author Ivan Krylov
 */
@Component
public final class GithubClient {
    private final AccessTokenProvider accessTokenProvider;
    private final LoginProvider loginProvider;
    private final WebClient githubApiWebClient;

    public GithubClient(final AccessTokenProvider accessTokenProvider,
                        final LoginProvider loginProvider,
                        final WebClient webClient) {
        this.accessTokenProvider = accessTokenProvider;
        this.githubApiWebClient = webClient
                .mutate()
                .baseUrl("https://api.github.com")
                .build();
        this.loginProvider = loginProvider;
    }

    public Mono<Boolean> hasAdminPermission(final String owner, final String repo) {
        return Mono
                .zip(accessTokenProvider.accessToken(), loginProvider.login())
                .flatMap(accessTokenAndLogin -> githubApiWebClient
                        .get()
                        .uri("/repos/{owner}/{repo}/collaborators/{user}/permission", owner, repo, accessTokenAndLogin.getT2())
                        .headers(h -> h.setBearerAuth(accessTokenAndLogin.getT1()))
                        .exchange()
                        .flatMap(response -> {
                            if (response.statusCode() != OK) {
                                return Mono.just(FALSE);
                            }

                            return response
                                    .bodyToMono(Permission.class)
                                    .map(permission -> permission.getPermission() == ADMIN);
                        })
                        .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    public Mono<Repo> getRepo(final String owner, final String repo) {
        return accessTokenProvider
                .accessToken()
                .flatMap(accessToken -> githubApiWebClient
                        .get()
                        .uri("/repos/{owner}/{repo}", owner, repo)
                        .headers(h -> h.setBearerAuth(accessToken))
                        .retrieve()
                        .bodyToMono(Repo.class)
                        .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    public Mono<Pull[]> getRepositoryOpenPulls(final String owner, final String repo) {
        return accessTokenProvider
                .accessToken()
                .flatMap(accessToken -> githubApiWebClient
                        .get()
                        .uri("/repos/{owner}/{repo}/pulls?state=open", owner, repo)
                        .headers(h -> h.setBearerAuth(accessToken))
                        .retrieve()
                        .bodyToMono(Pull[].class)
                        .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    public interface LoginProvider {
        Mono<String> login();
    }

    public interface AccessTokenProvider {
        Mono<String> accessToken();
    }
}
