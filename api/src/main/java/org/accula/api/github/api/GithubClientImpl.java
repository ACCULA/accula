package org.accula.api.github.api;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.github.model.GithubApiCollaborator;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUserPermission;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.BiFunction;
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

    public GithubClientImpl(final AccessTokenProvider accessTokenProvider, final LoginProvider loginProvider, final WebClient webClient) {
        this.accessTokenProvider = accessTokenProvider;
        this.loginProvider = loginProvider;
        this.githubApiWebClient = webClient
                .mutate()
                .baseUrl("https://api.github.com")
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10_000_000))
                        .build())
                .build();
    }

    @Override
    public Mono<Boolean> hasAdminPermission(final String owner, final String repo) {
        final BiFunction<String, String, Mono<ClientResponse>> requestUserPermissions = (accessToken, login) -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/collaborators/{user}/permission", owner, repo, login)
                .headers(h -> h.setBearerAuth(accessToken))
                .exchange();
        return accessTokenProvider.accessToken()
                .zipWith(loginProvider.login())
                .flatMap(TupleUtils.function(requestUserPermissions))
                .flatMap(response -> {
                    if (response.statusCode() != OK) {
                        return Mono.just(FALSE);
                    }

                    return response
                            .bodyToMono(GithubApiUserPermission.class)
                            .map(permission -> permission.getPermission() == ADMIN);
                })
                .onErrorResume(GithubClientException::wrap);
    }

    @Override
    public Mono<GithubApiRepo> getRepo(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiRepo.class)
                .onErrorResume(GithubClientException::wrap));
    }

    @Override
    public Mono<List<Long>> getRepoAdmins(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/collaborators", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToFlux(GithubApiCollaborator.class)
                .filter(GithubApiCollaborator::hasAdminPermissions)
                .map(GithubApiCollaborator::getId)
                .collectList()
                .onErrorResume(GithubClientException::wrap));
    }

    @Override
    public Mono<GithubApiPull[]> getRepositoryPulls(final String owner, final String repo, final GithubApiPull.State state) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls?&page=1&per_page=100&state=" + state.value(), owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull[].class)
                .onErrorResume(GithubClientException::wrap));
    }

    @Override
    public Mono<GithubApiPull> getRepositoryPull(final String owner, final String repo, final Integer pullNumber) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull.class)
                .onErrorResume(GithubClientException::wrap));
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
