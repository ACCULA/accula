package org.accula.api.handlers;

import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.accula.api.model.PullRequestModel;
import org.accula.api.model.WebHookModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Component
public class DataParserHandler {

    public Flux<GitPullRequest> getAllPR(@NotNull final String repo){
        String api = "https://api.github.com";
        WebClient cl = WebClient.create(api);
        return cl.get()
                //TODO: get all pr (now it is only one page by 30 pr)
                .uri("/repos/" + repo + "/pulls?state=all")
                .retrieve()
                .bodyToFlux(GitPullRequest.class);
    }

    public Flux<FileModel> getChangedFiles(@NotNull final String repo){
        WebClient cl = WebClient.create(repo);
        return cl.get()
                .uri("/files")
                .retrieve()
                //TODO: filter files by name
                .bodyToFlux(FileModel.class);
    }

    public Flux<GitPullRequest> getProject(@NotNull final String repo){
        Flux<GitPullRequest> pullRequests = getAllPR(repo);
        pullRequests.subscribe(pr -> {
           getChangedFiles(pr.getUrl()).collectList().subscribe(f -> {
               PullRequestModel pull_request = new PullRequestModel(pr,f);
               // only for debug
               System.out.println(pull_request.getAll());
           });
        });
        // TODO: add saving to database
        return pullRequests;
    }

  //  @NotNull
    public Mono<ServerResponse> getOldProjects(@NotNull final ServerRequest request) {
        //TODO: getting repo from request, working with more than one project
        getProject("polis-mail-ru/2017-highload-kv");
        return ok().build();
    }

    @NotNull
    public Mono<ServerResponse> getWebHookInformation(@NotNull final ServerRequest request) {
        // TODO: think about filtering pr by state (modified, added, merged)
        request.bodyToFlux(WebHookModel.class).subscribe(pr -> {
            getChangedFiles(pr.getPull_request().getUrl()).collectList().subscribe(f -> {
                PullRequestModel pull = new PullRequestModel(pr.getPull_request(),f);
                // only for debug
                System.out.println(pull.getAll());
            });
        });
    // TODO: add saving to database and starting clone detection
        return ok().build();
    }
}
