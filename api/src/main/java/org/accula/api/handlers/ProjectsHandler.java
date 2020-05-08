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

import java.net.URI;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
@RequiredArgsConstructor
public class ProjectsHandler {

    private final WebClient githubApiWebClient;
    private final UserRepository userRepository;
    private final String reposPath = "/repos/";
    private final String contributorsPath = "/contributors";

    public Mono<ServerResponse> addProject(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.bodyToMono(JsonNode.class))
                .flatMap(__ -> __)
                .map(node -> URI
                        .create(node
                                .get("url")
                                .asText())
                        .getPath())
                .flatMap(userRepoPath -> githubApiWebClient
                        .get()
                        .uri(reposPath + userRepoPath + contributorsPath)
                        .retrieve()
                        .bodyToMono(ArrayNode.class))
                .map(arrayNode -> arrayNode.findValuesAsText("login"))
                .flatMapMany(Flux::fromIterable)
                .zipWith(userRepository
                        .findById(ReactiveSecurityContextHolder
                                .getContext()
                                .map(SecurityContext::getAuthentication)
                                .cast(JwtAuthentication.class)
                                .map(JwtAuthentication::getPrincipal)
                                .map(AuthorizedUser::getId))
                        .map(User::getGithubLogin), String::equals)
                .any(bool -> bool.equals(Boolean.TRUE))
                .flatMap(jsonNodes -> ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(jsonNodes))
                .switchIfEmpty(badRequest().build());

    }

    public Mono<ServerResponse> getProjectById(final ServerRequest serverRequest) {
        return Mono.empty();
    }

    public Mono<ServerResponse> getAllProjects() {return Mono.empty();}

    public Mono<ServerResponse> updateProjectById(final ServerRequest serverRequest) {return Mono.empty();}

    public Mono<ServerResponse> deleteProjectById(final ServerRequest serverRequest) {return Mono.empty();}
}
