package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.handlers.response.GetPullResponseBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    //TODO: common handler for all NOT FOUND cases
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private final PullRepo pullRepo;
    private final ProjectRepository projects;
    private final GithubClient githubClient;

    //TODO: handle github errors
    public Mono<ServerResponse> getMany(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> Long.parseLong(request.pathVariable(PROJECT_ID)))
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .flatMap(projectId -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .body(pullRepo.findByProjectId(projectId), Pull.class))
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));

                    return pullRepo.findByNumber(projectId, pullNumber)
                            .switchIfEmpty(Mono.error(PULL_NOT_FOUND_EXCEPTION))
                            .flatMap(pull -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(pull));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    //TODO
    public Mono<ServerResponse> refresh(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable(PULL_NUMBER))
                //switchIfEmpty
                .map(Long::parseLong)
                //onError
                .flatMap(projects::findById)
                .then(Mono.empty());
        //return Mono
        //                .fromSupplier(() -> Long.parseLong(request.pathVariable(PROJECT_ID)))
        //                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
        //                .flatMap(projectId -> projects
        //                        .findById(projectId)
        //                        .flatMap(project -> githubClient.getRepositoryPulls(project.getRepoOwner(), project.getRepoName(), State.ALL))
        //                        .switchIfEmpty(Mono.error(PULL_NOT_FOUND_EXCEPTION))
        //                        .flatMapMany(Flux::fromArray)
        //                        .filter(GithubApiPull::isValid)
        //                        .flatMap(pull -> Mono.just(fromGithubPull(pull, projectId)))
        //                        .collectList()
        //                        .flatMap(githubPulls -> ServerResponse
        //                                .ok()
        //                                .contentType(APPLICATION_JSON)
        //                                .bodyValue(githubPulls)))
        //                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    private static GetPullResponseBody fromGithubPull(final GithubApiPull githubPull, final Long projectId) {
        return GetPullResponseBody.builder()
                .projectId(projectId)
                .number(githubPull.getNumber().intValue())
                .url(githubPull.getHtmlUrl())
                .title(githubPull.getTitle())
                .head(new GetPullResponseBody.PullRef(
                        githubPull.getHead().getTreeUrl(),
                        githubPull.getHead().getLabel()))
                .base(new GetPullResponseBody.PullRef(
                        githubPull.getBase().getTreeUrl(),
                        githubPull.getBase().getLabel()))
                .author(new GetPullResponseBody.PullAuthor(
                        githubPull.getUser().getLogin(),
                        githubPull.getUser().getAvatarUrl(),
                        githubPull.getUser().getHtmlUrl()))
                .open(githubPull.getState() == State.OPEN)
                .createdAt(githubPull.getCreatedAt())
                .updatedAt(githubPull.getUpdatedAt())
                .status(GetPullResponseBody.PullStatus.PENDING)
                .cloneCount(0)
                .build();
    }
}
