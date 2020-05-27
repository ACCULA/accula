package org.accula.api.github.api;

import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubRepo;
import org.accula.api.github.model.GithubUserPermission;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static org.accula.api.github.model.GithubUserPermission.Permission.ADMIN;
import static org.springframework.http.HttpStatus.OK;

/**
 * @author Anton Lamtev
 * @author Ivan Krylov
 */
@Component
public final class GithubClientImpl implements GithubClient {
    private final AccessTokenProvider accessTokenProvider;
    private final LoginProvider loginProvider;
    private final WebClient githubApiWebClient;

    public GithubClientImpl(final AccessTokenProvider accessTokenProvider,
                            final LoginProvider loginProvider,
                            final WebClient webClient) {
        this.accessTokenProvider = accessTokenProvider;
        this.githubApiWebClient = webClient
                .mutate()
                .baseUrl("https://api.github.com")
                .build();
        this.loginProvider = loginProvider;
    }

    @Override
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
                                    .bodyToMono(GithubUserPermission.class)
                                    .map(permission -> permission.getPermission() == ADMIN);
                        })
                        .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<GithubRepo> getRepo(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubRepo.class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<GithubPull[]> getRepositoryOpenPulls(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls?state=open", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubPull[].class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Flux<GithubPull> getRepositoryPulls(final String owner, final String repo, final Integer page) {
        return githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls?state=all&per_page=100&page={page}", owner, repo, page)
                .exchange()
                .flatMapMany(rs -> {
                    final List<String> headerLink = rs.headers().header("link");
                    //if repository contains more than 100 pull requests, request next page
                    if (!headerLink.isEmpty()) {
                        if (headerLink.get(0).contains("rel=\"next\"")) {
                            return rs.bodyToFlux(GithubPull.class)
                                    .concatWith(getRepositoryPulls(owner, repo, page + 1));
                        }
                    }
                    return rs.bodyToFlux(GithubPull.class);
                })
                .onErrorResume(e -> Flux.error(new GithubClientException(e)));
    }

    @Override
    public Mono<GithubPull> getRepositoryPull(final String owner, final String repo, final Integer pullNumber) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubPull.class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    private <T> Mono<T> withAccessToken(final Function<String, Mono<T>> transform) {
        return accessTokenProvider
                .accessToken()
                .flatMap(transform);
    }
}
