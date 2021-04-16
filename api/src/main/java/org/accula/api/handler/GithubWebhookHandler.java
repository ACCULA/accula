package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.PullSnapshots;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.github.model.GithubApiPullHookPayload;
import org.accula.api.github.model.GithubApiPushHookPayload;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.signature.Signatures;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.accula.api.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static org.accula.api.handler.GithubWebhookHandler.WebhookError.INVALID_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.NOT_SUPPORTED_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.SIGNATURE_VERIFICATION_FAILED;

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
    public static final String GITHUB_EVENT_PUSH = "push";
    public static final String GITHUB_SIGNATURE = "X-Hub-Signature-256";

    private final ProjectRepo projectRepo;
    private final ProjectService projectService;
    private final CloneDetectionService cloneDetectionService;
    private final WebhookProperties webhookProperties;

    public Mono<ServerResponse> webhook(final ServerRequest request) {
        final var headers = request.headers();
        final var event = headers.firstHeader(GITHUB_EVENT);
        if (event == null) {
            return badRequest(MISSING_EVENT);
        }
        final var signatureHeader = headers.firstHeader(GITHUB_SIGNATURE);
        if (signatureHeader == null) {
            return badRequest(MISSING_SIGNATURE);
        }
        final var signature = Strings.suffixAfterPrefix(signatureHeader, "sha256=");
        if (signature == null) {
            return badRequest(INVALID_SIGNATURE);
        }

        return request
            .bodyToMono(String.class)
            .flatMap(payload -> {
                if (!Signatures.checkHexHmacSha256(signature, webhookProperties.secret(), payload)) {
                    return badRequest(SIGNATURE_VERIFICATION_FAILED);
                }
                return switch (event) {
                    case GITHUB_EVENT_PING -> Responses.ok();
                    case GITHUB_EVENT_PULL -> processPull(request);
                    case GITHUB_EVENT_PUSH -> processPush(request);
                    default -> badRequest(NOT_SUPPORTED_EVENT);
                };
            });
    }

    private Mono<ServerResponse> processPull(final ServerRequest request) {
        return request
                .bodyToMono(GithubApiPullHookPayload.class)
                .onErrorResume(GithubWebhookHandler::ignoreNotSupportedAction)
                .flatMap(this::processPullPayload)
                .onErrorResume(e -> {
                    log.error("Error during payload processing: ", e);
                    return Mono.empty();
                })
                .then(Responses.ok());
    }

    private Mono<ServerResponse> processPush(final ServerRequest request) {
        return request
            .bodyToMono(GithubApiPushHookPayload.class)
            .flatMap(payload -> projectRepo
                .idByRepoId(payload.repo().id())
                .flatMap(projectId -> projectRepo.confById(projectId)
                    .filter(Project.Conf::keepsExcludedFilesSyncedWithGit)
                    .flatMap(conf -> projectService
                        .headFiles(GithubApiToModelConverter.convert(payload.repo()))
                        .map(conf::withExcludedFiles))
                    .flatMap(conf -> projectRepo.upsertConf(projectId, conf))))
            .then(Responses.ok());
    }

    private Mono<Void> processPullPayload(final GithubApiPullHookPayload payload) {
        return (switch (payload.action()) {
            case OPENED, SYNCHRONIZE -> updateProject(payload)
                .doOnNext(update -> detectClonesInBackground(update.getT1(), update.getT2()));
            case CLOSED, REOPENED,
                 EDITED,
                 ASSIGNED, UNASSIGNED -> projectService.updatePullInfo(payload.pull());
        }).then();
    }

    private Mono<Tuple2<Long, PullSnapshots>> updateProject(final GithubApiPullHookPayload payload) {
        return projectRepo
            .idByRepoId(payload.repo().id())
            .flatMap(projectId -> Mono
                .just(projectId)
                .zipWith(projectService.updateWithNewCommits(payload.pull())));
    }

    private void detectClonesInBackground(final Long projectId, final PullSnapshots pullSnapshots) {
        cloneDetectionService
            .detectClones(projectId, pullSnapshots.pull(), pullSnapshots.snapshots())
            .subscribe();
    }

    private static <E extends Throwable, T> Mono<T> ignoreNotSupportedAction(final E error) {
        return Mono.empty();
    }

    private static Mono<ServerResponse> badRequest(final WebhookError error) {
        return Responses.badRequest(ApiError.with(error));
    }

    public enum WebhookError implements ApiError.Code {
        MISSING_EVENT,
        MISSING_SIGNATURE,
        INVALID_SIGNATURE,
        SIGNATURE_VERIFICATION_FAILED,
        NOT_SUPPORTED_EVENT,
    }
}
