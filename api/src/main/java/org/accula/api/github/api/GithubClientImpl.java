package org.accula.api.github.api;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.github.model.GithubApiCollaborator;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUserPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.function.Predicate.not;
import static org.accula.api.github.model.GithubApiUserPermission.Permission.ADMIN;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Slf4j
@Component
public final class GithubClientImpl implements GithubClient {
    private static final MediaType APPLICATION_VND_GITHUB_V3_JSON = new MediaType("application", "vnd.github.v3+json");
    private final AccessTokenProvider accessTokenProvider;
    private final LoginProvider loginProvider;
    private final WebClient githubApiWebClient;

    public GithubClientImpl(final AccessTokenProvider accessTokenProvider, final LoginProvider loginProvider, final WebClient webClient) {
        this.accessTokenProvider = () -> accessTokenProvider
                .accessToken()
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("Access token MUST be present")));
        this.loginProvider = () -> loginProvider
                .login()
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("Login MUST be present")));
        this.githubApiWebClient = webClient
                .mutate()
                .baseUrl("https://api.github.com")
                .defaultHeaders(h -> h.setAccept(List.of(APPLICATION_VND_GITHUB_V3_JSON)))
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10_000_000))
                        .build())
                .build();
    }

    @Override
    public Mono<Boolean> hasAdminPermission(final String owner, final String repo) {
        final BiFunction<String, String, Mono<GithubApiUserPermission>> requestUserPermissions = (accessToken, login) -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/collaborators/{user}/permission", owner, repo, login)
                .headers(h -> h.setBearerAuth(accessToken))
                .exchangeToMono(response -> response.bodyToMono(GithubApiUserPermission.class));
        return accessTokenProvider.accessToken()
                .zipWith(loginProvider.login())
                .flatMap(TupleUtils.function(requestUserPermissions))
                .filter(permission -> permission.permission() == ADMIN)
                .hasElement()
                .onErrorMap(GithubClientException::new);
    }

    @Override
    public Mono<GithubApiRepo> getRepo(final String owner, final String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiRepo.class)
                .onErrorMap(GithubClientException::new));
    }

    @Override
    public Flux<GithubApiRepo> getAllRepos(final int perPage) {
        final var page = new AtomicInteger(1);
        return getAllRepos(perPage, page.getAndIncrement())
            .expand(repos -> {
                if (repos.length < perPage) {
                    return Mono.empty();
                }
                return getAllRepos(perPage, page.getAndIncrement());
            })
            .flatMap(Flux::fromArray);
    }

    @Override
    public Mono<GithubApiRepo[]> getAllRepos(final int perPage, final int page) {
        return withAccessToken(accessToken -> githubApiWebClient
            .get()
            .uri("/user/repos?per_page={perPage}&page={page}&type=all", perPage, page)
            .headers(h -> h.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(GithubApiRepo[].class)
            .onErrorMap(GithubClientException::new));
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
                .map(GithubApiCollaborator::id)
                .collectList()
                .onErrorMap(GithubClientException::new));
    }

    @Override
    public Flux<GithubApiPull> getRepositoryPulls(final String owner,
                                                  final String repo,
                                                  final GithubApiPull.State state,
                                                  final int perPage) {
        final var page = new AtomicInteger(1);
        return getRepositoryPulls(owner, repo, state, perPage, page.getAndIncrement())
                .expand(pulls -> {
                    if (pulls.length < perPage) {
                        return Mono.empty();
                    }
                    return getRepositoryPulls(owner, repo, state, perPage, page.getAndIncrement());
                })
                .flatMap(Flux::fromArray);
    }

    @Override
    public Mono<GithubApiPull[]> getRepositoryPulls(final String owner,
                                                    final String repo,
                                                    final GithubApiPull.State state,
                                                    final int perPage,
                                                    final int page) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls?state={state}&per_page={perPage}&page={page}", owner, repo, state.value(), perPage, page)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull[].class)
                .onErrorMap(GithubClientException::new));
    }

    @Override
    public Mono<GithubApiPull> getRepositoryPull(final String owner, final String repo, final Integer pullNumber) {
        return withAccessToken(accessToken -> githubApiWebClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubApiPull.class)
                .onErrorMap(GithubClientException::new));
    }

    @Override
    public Mono<List<GithubApiHook>> listHooks(String owner, String repo) {
        return withAccessToken(accessToken -> githubApiWebClient
            .get()
            .uri("/repos/{owner}/{repo}/hooks", owner, repo)
            .headers(h -> h.setBearerAuth(accessToken))
            .retrieve()
            .onStatus(not(HttpStatus.OK::equals), ClientResponse::createException)
            .bodyToMono(GithubApiHook[].class)
            .map(List::of))
            .onErrorMap(GithubClientException::new);
    }

    @Override
    public Mono<GithubApiHook> createHook(final String owner, final String repo, final GithubApiHook hook) {
        return withAccessToken(accessToken -> githubApiWebClient
            .post()
            .uri("/repos/{owner}/{repo}/hooks", owner, repo)
            .headers(h -> h.setBearerAuth(accessToken))
            .bodyValue(hook)
            .retrieve()
            .onStatus(not(HttpStatus.CREATED::equals), ClientResponse::createException)
            .bodyToMono(GithubApiHook.class)
            .doOnSuccess(p -> log.info("Created GitHub webhook for {}/{}", owner, repo))
            .doOnError(e -> log.error("Cannot create hook {} for {}/{}", hook, owner, repo, e)))
            .onErrorMap(GithubClientException::new);
    }

    @Override
    public Mono<Void> deleteHook(String owner, String repo, Integer hookId) {
        return withAccessToken(accessToken -> githubApiWebClient
            .delete()
            .uri("/repos/{owner}/{repo}/hooks/{hook_id}", owner, repo, hookId)
            .retrieve()
            .onStatus(not(HttpStatus.NO_CONTENT::equals), ClientResponse::createException)
            .bodyToMono(Void.class)
            .doOnSuccess(p -> log.info("Deleted GitHub webhook with id {} for {}/{}", hookId, owner, repo))
            .doOnError(e -> log.error("Cannot delete hook with id {} for {}/{}", hookId, owner, repo, e)))
            .onErrorMap(GithubClientException::new);
    }

    private <T> Mono<T> withAccessToken(final Function<String, Mono<T>> transform) {
        return accessTokenProvider
                .accessToken()
                .flatMap(transform);
    }
}
