package org.accula.api.handlers;

import org.accula.api.model.FileModel;
import org.accula.api.model.PullRequestModel;
import org.accula.api.model.WebHookModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class DataParserHandler {
    private static final String TOKEN = "5075f864c06cefe723555092d1fb8c342b67e3d4";

    public Flux<PullRequestModel> getAllPR(@NotNull final String repo){
        String API = "https://api.github.com";
        WebClient cl = WebClient.create(API);
        return cl.get()
                //TODO: get all pr (now it is only one page by 30 pr)
                .uri("/repos/" + repo + "/pulls?state=all")
                .headers(headers -> headers.setBearerAuth(TOKEN))
                .retrieve()
                .bodyToFlux(PullRequestModel.class);
    }

    public Flux<FileModel> getChangedFiles(@NotNull final String repo){
        WebClient cl = WebClient.create(repo);
        return cl.get()
                .uri("/files")
                .headers(headers -> headers.setBearerAuth(TOKEN))
                .retrieve()
                //TODO: filter files by name
                .bodyToFlux(FileModel.class);
    }
    public Flux<PullRequestModel> getProject(@NotNull final String repo){
        Flux<PullRequestModel> pullRequests = getAllPR(repo);
        pullRequests.subscribe(pr -> {
           getChangedFiles(pr.getUrl()).collectList().subscribe(f -> {
               pr.setChanged_files(f);
               // only for debug
               System.out.println(pr.getAll());
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
            getChangedFiles(pr.getPRUrl()).collectList().subscribe(f -> {
                PullRequestModel pull = pr.formPullRequestModel();
                pull.setChanged_files(f);
                // only for debug
                System.out.println(pull.getAll());
            });
        });
    // TODO: add saving to database and starting clone detection
        return ok().build();
    }
}
