package org.accula.api.handlers;

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

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Component
public class DataParserHandler {

    public Flux<GitPullRequest> getAllPR(final String repo, final Integer page){
        final String api = "https://api.github.com";
        final String nextPage = "rel=\"next\"";
        //TODO add authorization to get 5000 requests per hour (now is only 60)
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

    public Flux<FileModel> getChangedFiles(final String repo, final Integer page){
        final String nextPage = "rel=\"next\"";
        //TODO add authorization to get 5000 requests per hour (now is only 60)
        return WebClient.create(repo).get()
                .uri("/files?per_page=100&page=" + page)
                .exchange()
                .flatMapMany(rs -> {
                    //if pull request contains more than 100 changed files, request next page
                    List<String> headerLink = rs.headers().header("link");
                    if (!headerLink.isEmpty()){
                        if (headerLink.get(0).contains(nextPage)) {
                            return rs.bodyToFlux(FileModel.class)
                                    .concatWith(getChangedFiles(repo, page + 1));
                        }
                    }
                    return rs.bodyToFlux(FileModel.class);
                })
                .onErrorResume(e -> Flux.empty());
    }

    public Flux<FileModel> getFilesContent(Flux<FileModel> files, GitPullRequest pr){
        final String fileType = ".java";
        //TODO: add getting files to filer from database
        final String[] filesToFilter = {};
        //final String[] filesToFilter = getFilesFromDB();
        return files
                .filter(f -> (!Arrays.asList(filesToFilter).contains(f.getFilename()))&&
                        (f.getFilename().endsWith(fileType)))
                .flatMap(file -> WebClient.create(file.getContents_url()).get()
                        .retrieve()
                        //TODO: add error filtering
                        .bodyToMono(FileModel.class)
                        .flatMap(fl -> Mono.just(new FileModel(file.getFilename(),
                                file.getStatus(),
                                file.getBlob_url(),file.getContents_url(),
                                pr.getUser(), pr.getCreated_at(),
                                new String(Base64.getMimeDecoder().decode(fl.getContent()))))
                        )
                )
                .onErrorResume(e -> Flux.empty());
    }

    public Flux<FileModel> getProject(final String repo, final String studentName){
        //get pull requests for this repository
        return getAllPR(repo,1)
                //TODO: think about filtering by date
                .filter(pr -> !pr.getUser().getLogin().equals(studentName))
                .flatMap(pr -> getFilesContent(getChangedFiles(pr.getUrl(), 1), pr))
                .onErrorResume(e -> Flux.empty());
    }

    public Mono<ServerResponse> getWebHookInformation(final ServerRequest request) {
        request.bodyToFlux(WebHookModel.class).subscribe(studentPR -> {
            Flux<FileModel> studentFiles = getFilesContent(
                    getChangedFiles(studentPR.getPull_request().getUrl(), 1),
                    studentPR.getPull_request());
            //TODO: getting repos from db
            final String[] repos = {"ACCULA/accula"};
            //final String[] repos = getReposFromBD();
            Flux<FileModel> filesToCompare = Flux.fromArray(repos)
                    .filter(r -> !r.isEmpty())
                    .flatMap(repo -> getProject(repo, studentPR.getPull_request().getUser().getLogin()));
            //TODO: start clone analyzing
            //analyze(studentFiles, filesToCompare);
        });
        // I think we will always have timeout for response, if we'll start analyzing in this method, but where
        // should we call analyzer?
        return ok().build();
    }
}
