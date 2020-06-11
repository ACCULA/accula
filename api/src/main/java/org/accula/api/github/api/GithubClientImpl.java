package org.accula.api.github.api;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUserPermission;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static org.accula.api.github.model.GithubApiUserPermission.Permission.ADMIN;
import static org.springframework.http.HttpStatus.OK;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Slf4j
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
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10_000_000))
                        .build())
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
                                    .bodyToMono(GithubApiUserPermission.class)
                                    .map(permission -> permission.getPermission() == ADMIN);
                        })
                        .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<GithubApiRepo> getRepo(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiRepo.class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<GithubApiPull[]> getRepositoryPulls(final String owner, final String repo, final GithubApiPull.State state) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls?&page=1&per_page=100&state=" + state.value(), owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull[].class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<GithubApiPull> getRepositoryPull(final String owner, final String repo, final Integer pullNumber) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull.class)
                .onErrorResume(e -> Mono.error(new GithubClientException(e))));
    }

    @Override
    public Mono<Void> createHook(final String owner, final String repo, final GithubApiHook hook) {
        return withAccessToken(accessToken -> githubApiWebClient
                .post()
                .uri("/repos/{owner}/{repo}/hooks", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(hook)
                .exchange()
                .doOnSuccess(p -> log.info("Created GitHub webhook for {}/{}", owner, repo))
                .doOnError(e -> log.error("Cannot create hook for {}/{}", owner, repo, e))
                .then());
    }

    private <T> Mono<T> withAccessToken(final Function<String, Mono<T>> transform) {
        return accessTokenProvider
                .accessToken()
                .flatMap(transform);
    }
}
