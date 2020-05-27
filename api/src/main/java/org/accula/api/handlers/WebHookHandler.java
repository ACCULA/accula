package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.files.Files;
import org.accula.api.files.model.FileModel;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubWebhook;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebHookHandler {

    private final GithubClient githubClient;

    public Mono<ServerResponse> getWebHookInformation(final ServerRequest request) {
        //TODO: get files from db;
        final Set<String> excludeFiles = Set.of("TestBase.java", "StartStopTest.java");
        //TODO: get repos from db
        final Set<Map.Entry<String,String>> repos = Set.of(new AbstractMap.SimpleEntry<>("polis-mail-ru", "2017-highload-kv"),
                new AbstractMap.SimpleEntry<>("polis-mail-ru", "2019-highload-dht"));

        request.bodyToMono(GithubWebhook.class)
                .publishOn(Schedulers.elastic())
                .subscribe(p -> {
                    final GithubPull pr = p.getPullRequest();
                    final Flux<FileModel> studentFiles = Files.getPRFiles(pr, excludeFiles);
                    final Flux<FileModel> otherFiles = Flux.fromIterable(repos)
                            .flatMap(repo -> githubClient.getRepositoryPulls(repo.getKey(),repo.getValue(),1))
                            .filter(pull -> !pull.getUser().getLogin().equals(pr.getUser().getLogin()))
                            .flatMap(pull -> Files.getPRFiles(pull, excludeFiles));
                    //TODO: start clone analyzing
        });
        return ok().build();
    }
}
