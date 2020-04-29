package org.accula.data.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.data.model.GFile;
import org.accula.data.model.PullRequest;
import org.accula.data.model.RepositoryContent;
import org.accula.data.provider.filter.GFileFilter;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

@Slf4j
@RequiredArgsConstructor
public class GitHubClient implements DataProvider<PullRequest, GFile<String>> {
    @NotNull
    private final String source;
    @NotNull
    private final String token;
    @NotNull
    private final WebClient repositoryClient = WebClient.create();
    @NotNull
    private final WebClient fileDownloaderClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().followRedirect(true)))
            .build();

    @NotNull
    private String getApiSource() {
        var tmp = source.split("/");
        return String.format("https://api.github.com/repos/%s/%s/pulls", tmp[3], tmp[4]);
    }

    // TODO: Add pagination support and PR filter
    @NotNull
    @Override
    public Flux<PullRequest> fetchPullRequests() {
        return WebClient.
                create(getApiSource())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("state", "all")
                        .queryParam("per_page", "100")
                        .build())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PullRequest.class)
                .doOnError(e -> log.error("Error fetching PRs: {}", e.getMessage()));
    }

    @NotNull
    @Override
    public Flux<GFile<String>> fetchRepoContent(@NotNull final Integer prNumber,
                                                @NotNull final String userName,
                                                @NotNull final GFileFilter filter) {
        var link = String.format("%s/%d/files?per_page=100", getApiSource(), prNumber);
        return repositoryClient
                .get()
                .uri(link)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(RepositoryContent.class)
                .concatMap(rc -> downloadFile(rc, userName, prNumber, filter).subscribeOn(Schedulers.single()))
                .doOnError(e -> log.error("Error fetching repository {}/{}: {}",userName, prNumber, e.getMessage()));
    }

    @NotNull
    private Mono<GFile<String>> downloadFile(@NotNull final RepositoryContent rc,
                                             @NotNull final String userName,
                                             @NotNull final Integer prNumber,
                                             @NotNull final GFileFilter filter) {
        if (filter.accept(rc.getPath())) {
            return fileDownloaderClient
                    .get()
                    .uri(rc.getRawUrl())
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(resp -> new GFile<>(userName, prNumber, rc.getFilename(), rc.getLinkToFile(), resp))
                    .doOnError(e -> log.error("Error downloading file {}: {}", rc.getFilename(), e.getMessage()));
        } else return Mono.empty();
    }
}
