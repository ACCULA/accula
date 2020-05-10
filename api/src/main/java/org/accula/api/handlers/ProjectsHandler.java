package org.accula.api.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.AuthorizedUser;
import org.accula.api.auth.jwt.JwtAuthentication;
import org.accula.api.db.UserRepository;
import org.accula.api.db.model.User;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
@RequiredArgsConstructor
public class ProjectsHandler {

    private final WebClient githubApiWebClient;
    private final UserRepository userRepository;
    private static final String REPOS_PATH = "/repos/";
    private static final String CONTRIBUTORS_PATH = "/contributors";

    private Mono<ArrayNode> getGithubRepositoryContributorsByPath(final String path) {
        return githubApiWebClient
                .get()
                .uri(REPOS_PATH + path + CONTRIBUTORS_PATH)
                .retrieve()
                .bodyToMono(ArrayNode.class);
    }

    private Mono<JsonNode> getGithubRepositoryInfoByPath(final String path) {
        return githubApiWebClient
                .get()
                .uri(REPOS_PATH + path)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<ServerResponse> addProject(final ServerRequest request) {

        final Mono<String> userRepoPath = Mono
                .justOrEmpty(request.bodyToMono(JsonNode.class))
                .flatMap(jsonNodeMono ->  jsonNodeMono)
                .map(node -> URI
                        .create(node
                                .get("url")
                                .asText())
                        .getPath());

        return userRepoPath
                .map(path -> Tuples.of(path, getGithubRepositoryContributorsByPath(path)))
                .map(tuple2 -> tuple2
                        .mapT2(arrayNodeMono -> arrayNodeMono
                                .map(arrayNode -> arrayNode
                                        .findValuesAsText("login"))))
                .map(tuple2 -> tuple2
                        .mapT2(listMono -> listMono
                                .flatMapMany(Flux::fromIterable)))
                .map(tuple2 -> tuple2
                        .mapT2(stringFlux -> stringFlux
                                .zipWith(userRepository
                                        .findById(ReactiveSecurityContextHolder
                                                .getContext()
                                                .map(SecurityContext::getAuthentication)
                                                .cast(JwtAuthentication.class)
                                                .map(JwtAuthentication::getPrincipal)
                                                .map(AuthorizedUser::getId))
                                        .map(User::getGithubLogin), String::equals)))
                .map(tuple2 -> tuple2
                        .mapT2(booleanFlux -> booleanFlux
                                .any(bool -> bool
                                        .equals(Boolean.TRUE))))
                .flatMap(tuple2 -> tuple2
                        .getT2()
                                .zipWith(Mono.just(tuple2.getT1()), (isContributor, path) -> {
                                    if (isContributor)
                                        return getGithubRepositoryInfoByPath(path);
                                    else
                                        return Mono.empty();
                                }))
                .flatMap(mono -> mono)
                .flatMap(result -> ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .switchIfEmpty(badRequest().build());

    }

    public Mono<ServerResponse> getProjectById(final ServerRequest serverRequest) {
        return Mono.empty();
    }

    public Mono<ServerResponse> getAllProjects() {return Mono.empty();}

    public Mono<ServerResponse> updateProjectById(final ServerRequest serverRequest) {return Mono.empty();}

    public Mono<ServerResponse> deleteProjectById(final ServerRequest serverRequest) {return Mono.empty();}
}
