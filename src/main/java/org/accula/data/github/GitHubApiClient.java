package org.accula.data.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.data.DataProvider;
import org.accula.parser.FileEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE;
import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE;

@Slf4j
@RequiredArgsConstructor
public class GitHubApiClient implements DataProvider {
    private final String source;
    private final String token;
    private final WebClient repositoryClient = WebClient.create();
    private final WebClient fileDownloaderClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();

    @Override
    public Flux<FileEntity> getFiles() {
        final var start = System.nanoTime();

        final var pulls = fetchPullRequests()
                .take(100)
                .publish()
                .autoConnect(2);
        pulls
                .count()
                .subscribe(x -> System.err.println("Fetched " + x + " pull requests from " + source));
        final var files = pulls
                .concatMap(pr -> fetchPullRequestContent(pr.getNumber(), pr.getUserName()))
                .publish()
                .autoConnect(2);
        files
                .count()
                .doFinally(endType -> System.err.println(
                        "Fetching time : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms."
                ))
                .subscribe(x -> System.err.println("Fetched " + x + " files from " + source));

        return files;
    }

    private String getApiSource() {
        var tmp = source.split("/");
        return String.format("https://api.github.com/repos/%s/%s/pulls", tmp[3], tmp[4]);
    }

    // TODO: Add pagination support and PR filter (?)
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
                .publishOn(Schedulers.newBoundedElastic(
                        DEFAULT_BOUNDED_ELASTIC_SIZE,
                        DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                        String.format("%s-boundedElastic", getClass().getSimpleName()),
                        60,
                        true
                ))
                .doOnError(e -> log.error("Error fetching PRs: ", e));
    }

    // TODO: Replace with JGit repo downloader (?), add pagination support (?)
    public Flux<FileEntity> fetchPullRequestContent(@NotNull final Integer prNumber,
                                                    @NotNull final String userName) {
        var link = String.format("%s/%d/files?per_page=100", getApiSource(), prNumber);
        return repositoryClient
                .get()
                .uri(link)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(RepositoryFile.class)
                .flatMap(rc -> downloadFile(rc, userName).subscribeOn(Schedulers.newSingle("file-downloader")))
                .doOnError(e -> log.error("Error fetching files from {}[{}]: ", userName, prNumber, e));
    }

    private Mono<FileEntity> downloadFile(@NotNull final RepositoryFile rf, @NotNull final String userName) {
        if (rf.getPath().endsWith("java")) {
            System.err.println("Downloading : " + rf.getFilename());
            return fileDownloaderClient
                    .get()
                    .uri(rf.getRawUrl())
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(resp -> new FileEntity(rf.getFilename(), rf.getLinkToFile(), userName, resp))
                    .doOnError(e -> log.error("Error downloading file {}: ", rf.getFilename(), e));
        } else return Mono.empty();
    }
}
