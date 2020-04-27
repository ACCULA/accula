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

import java.util.Arrays;

@Component
public class DataParserHandler {

    public Flux<GitPullRequest> getAllPR(@NotNull final String repo, @NotNull final Integer page){
        String api = "https://api.github.com";
        WebClient cl = WebClient.create(api);
        /*TODO add authorization to get 5000 requests per hour (now is only 60)
        it will be better to get auth token from user, because request quota is sharing between tokens of one user
         */
        return cl.get()
                .uri("/repos/" + repo + "/pulls?state=all&per_page=100&page=" + page)
                .exchange()
                //all incorrect answers (incorrect repository name or expired rate limit) are filtered out
                .filter(r -> !r.headers().header("status").isEmpty())
                .filter(r -> r.headers().header("status").get(0).equals("200 OK"))
                .flatMapMany(rs -> {
                    //if repository contains more than 100 pull requests, request next page
                    if (!rs.headers().header("link").isEmpty()) {
                        if (rs.headers().header("link").get(0).contains("rel=\"next\""))
                            return rs.bodyToFlux(GitPullRequest.class).concatWith(getAllPR(repo, page + 1));
                    }
                    return rs.bodyToFlux(GitPullRequest.class);
                });
    }

    public Flux<FileModel> getChangedFiles(@NotNull final String repo, @NotNull final Integer page){
        WebClient cl = WebClient.create(repo);
        /*TODO add authorization to get 5000 requests per hour (now is only 60)
        it will be better to get auth token from user, because request quota is sharing between tokens of one user
         */
        return cl.get()
                .uri("/files?per_page=100&page=" + page)
                .exchange()
                //all incorrect answers (incorrect repository name or expired rate limit) are filtered out
                .filter(r -> !r.headers().header("status").isEmpty())
                .filter(r -> r.headers().header("status").get(0).equals("200 OK"))
                .flatMapMany(rs -> {
                    //if pull request contains more than 100 changed files, request next page
                    if (!rs.headers().header("link").isEmpty()){
                        if (rs.headers().header("link").get(0).contains("rel=\"next\""))
                            return rs.bodyToFlux(FileModel.class).concatWith(getChangedFiles(repo, page + 1));
                    }
                    return rs.bodyToFlux(FileModel.class);
                });
    }

    public Flux<PullRequestModel> getProject(@NotNull final String repo, final String[] files){
        //get pull requests for this repository
        return getAllPR(repo,1)
                .flatMap(pr ->
                //get files was changed in pull request
                        getChangedFiles(pr.getUrl(), 1)
                            .filter(f -> !Arrays.asList(files).contains(f.getFilename()))
                            .collectList()
                            .flatMap(f -> Mono.just(new PullRequestModel(pr, f)))
                );
    }

    @NotNull
    public Mono<ServerResponse> getOldProjects(@NotNull final ServerRequest request) {
        //getting list of files to filter pull requests, files must be divided by ","
        String[] files = request.queryParam("files").isPresent() ?
                request.queryParam("files").get().split(","): new String[]{};
        String repos = request.queryParam("repos").isPresent() ?
                request.queryParam("repos").get(): new String();
        if(repos.isEmpty())
            return ok().bodyValue("No repositories to parse data");
        Flux.fromArray(repos.split(","))
                .filter(r -> !r.isEmpty())
                .flatMap(repo -> {
                            // TODO: add saving pull requests to database
                            return getProject(repo, files);
                        }
                ).log().subscribe();
        //TODO: response depending on real state (all repositories saved or not)
        return ok().bodyValue("Saved");
    }

    @NotNull
    public Mono<ServerResponse> getWebHookInformation(@NotNull final ServerRequest request) {
        // TODO: think about filtering pr by state (modified, added, merged)
        //TODO: add getting files to filer from database
        request.bodyToFlux(WebHookModel.class).flatMap(pr ->
                getChangedFiles(pr.getPull_request().getUrl(), 1)
                     //   .filter(f -> !Arrays.asList(files).contains(f.getFilename()))
                        .collectList()
                        .flatMap(f ->
                                // TODO: add saving to database and starting clone detection
                                Mono.just(new PullRequestModel(pr.getPull_request(), f))
                        )
        ).log().subscribe();
        //TODO: response depending on real state (all repositories saved or not)
        return ok().bodyValue("Saved");
    }
}
