package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.github.model.GithubApiHookPayload;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.accula.api.db.model.PullSnapshots;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Objects;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class GithubWebhookHandler {
    public static final String GITHUB_EVENT = "X-GitHub-Event";
    public static final String GITHUB_EVENT_PING = "ping";
    public static final String GITHUB_EVENT_PULL = "pull_request";

    private final ProjectRepo projectRepo;
    private final ProjectService projectService;
    private final CloneDetectionService cloneDetectionService;

    public Mono<ServerResponse> webhook(final ServerRequest request) {
        // TODO: validate signature in X-Hub-Signature
        return switch (Objects.requireNonNull(request.headers().firstHeader(GITHUB_EVENT))) {
            case GITHUB_EVENT_PING -> Responses.ok();
            case GITHUB_EVENT_PULL -> processPull(request);
            default -> Responses.badRequest();
        };
    }

    private Mono<ServerResponse> processPull(final ServerRequest request) {
        return request
                .bodyToMono(GithubApiHookPayload.class)
                .onErrorResume(GithubWebhookHandler::ignoreNotSupportedAction)
                .flatMap(this::processPayload)
                .onErrorResume(e -> {
                    log.error("Error during payload processing: ", e);
                    return Mono.empty();
                })
                .flatMap(Lambda.expandingWithArg(Responses::ok));
    }

    private Mono<Void> processPayload(final GithubApiHookPayload payload) {
        return (switch (payload.action()) {
            case OPENED, SYNCHRONIZE -> updateProject(payload)
                .doOnNext(update -> detectClonesInBackground(update.getT1(), update.getT2()));
            case CLOSED, REOPENED,
                 EDITED,
                 ASSIGNED, UNASSIGNED -> projectService.simpleUpdate(payload.pull());
        }).then();
    }

    private Mono<Tuple2<Long, PullSnapshots>> updateProject(final GithubApiHookPayload payload) {
        return projectRepo
            .idByRepoId(payload.repo().id())
            .flatMap(projectId -> Mono
                .just(projectId)
                .zipWith(projectService.update(payload.pull())));
    }

    private void detectClonesInBackground(final Long projectId, final PullSnapshots pullSnapshots) {
        cloneDetectionService
            .detectClones(projectId, pullSnapshots.pull(), pullSnapshots.snapshots())
            .subscribe();
    }

    private static <E extends Throwable, T> Mono<T> ignoreNotSupportedAction(final E error) {
        return Mono.empty();
    }
}
