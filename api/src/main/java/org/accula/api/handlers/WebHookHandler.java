package org.accula.api.handlers;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.accula.api.model.GitUserModel;
import org.accula.api.model.WebHookModel;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Component
public class WebHookHandler {
    public Mono<ServerResponse> getWebHookInformation(final ServerRequest request) {
        final Mono<GitPullRequest> pullRequestInfo = request.bodyToMono(WebHookModel.class).flatMap(studentPR -> {
            final String userUrl = studentPR.getPullRequest().getUser().getUrl();
            return WebClient.create(userUrl)
                    .get()
                    .retrieve()
                    .bodyToMono(GitUserModel.class)
                    .flatMap(st -> {
                        GitPullRequest pr = studentPR.getPullRequest();
                        pr.setUser(st);
                        return Mono.just(pr);
                    });
        });
        //TODO: get files from db;
        final Set<String> excludeFiles = Set.of("TestBase.java", "StartStopTest.java");
        //TODO: get repos from db
        final Set<String> repos = Set.of("ACCULA/accula", "polis-mail-ru/2017-highload-kv");
        pullRequestInfo.subscribe(pr -> {
            final Flux<FileModel> studentFiles = Files.getStudentFiles(pr, excludeFiles);
            final Flux<FileModel> otherFiles = Flux.fromIterable(repos)
                    .flatMap(repo -> Files.getOtherFiles(pr, excludeFiles, repo));
            //TODO: start clone analyzing
        });

        return ok().build();
    }
}
