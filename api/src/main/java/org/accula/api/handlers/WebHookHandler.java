package org.accula.api.handlers;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.accula.api.model.WebHookModel;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Slf4j
@Component
public class WebHookHandler {
    public Mono<ServerResponse> getWebHookInformation(final ServerRequest request) {
        //TODO: get files from db;
        final Set<String> excludeFiles = Set.of("TestBase.java", "StartStopTest.java");
        //TODO: get repos from db
        final Set<String> repos = Set.of("polis-mail-ru/2017-highload-kv", "polis-mail-ru/2019-highload-dht");

        request.bodyToMono(WebHookModel.class)
                .publishOn(Schedulers.elastic())
                .subscribe(p -> {
                    final GitPullRequest pr = p.getPullRequest();
                    final Flux<FileModel> studentFiles = Files.getRepoFiles(pr, excludeFiles);
                    final Flux<FileModel> otherFiles = Flux.fromIterable(repos)
                            .flatMap(repo -> getAllPR(repo,1))
                            .filter(pull -> !pull.getUser().getLogin().equals(pr.getUser().getLogin()))
                            .flatMap(pull -> Files.getRepoFiles(pull, excludeFiles));
                    //TODO: start clone analyzing
        });
        return ok().build();
    }

    public Flux<GitPullRequest> getAllPR(final String repo, final Integer page){
        final String api = "https://api.github.com";
        final String nextPage = "rel=\"next\"";
        return WebClient.create(api).get()
                .uri("/repos/" + repo + "/pulls?state=all&per_page=100&page=" + page)
                .exchange()
                .flatMapMany(rs -> {
                    List<String> headerLink = rs.headers().header("link");
                    //if repository contains more than 100 pull requests, request next page
                    if (!headerLink.isEmpty()) {
                        if (headerLink.get(0).contains(nextPage)) {
                            return rs.bodyToFlux(GitPullRequest.class)
                                    .concatWith(getAllPR(repo, page + 1));
                        }
                    }
                    return rs.bodyToFlux(GitPullRequest.class);
                })
                .onErrorResume(e -> Flux.empty());
    }
}
