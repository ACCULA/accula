package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.SnippetMarker;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.dto.CloneDto;
import org.accula.api.handler.dto.CloneDto.FlatCodeSnippet.FlatCodeSnippetBuilder;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.util.Lambda;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class ClonesHandler {
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private static final Base64.Encoder base64 = Base64.getEncoder(); // NOPMD

    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CurrentUserRepo currentUserRepo;
    private final ProjectRepo projectRepo;
    private final CloneDetectionService cloneDetectionService;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> getLastCommitClones(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    return getLastCommitClones(projectId, pullNumber);
                })
                .onErrorResume(NumberFormatException.class, Lambda.expandingWithArg(Responses::badRequest));
    }

    public Mono<ServerResponse> refreshClones(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));

                    final var clones = doIfCurrentUserHasAdminPermissionInProject(projectId, cloneRepo
                            .deleteByPullNumber(projectId, pullNumber)
                            .thenMany(pullRepo
                                    .findByNumber(projectId, pullNumber)
                                    .flatMapMany(cloneDetectionService::detectClones)))
                            .cache();
                    return toResponse(clones, projectId, pullNumber)
                            .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(NumberFormatException.class, Lambda.expandingWithArg(Responses::badRequest));
    }

    private <T> Flux<T> doIfCurrentUserHasAdminPermissionInProject(final long projectId, final Flux<T> action) {
        return currentUserRepo
                .get(User::id)
                .filterWhen(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId))
                .flatMapMany(currentUserId -> action);
    }

    private Mono<ServerResponse> getLastCommitClones(final long projectId, final int pullNumber) {
        final var pullHead = pullRepo
                .findByNumber(projectId, pullNumber)
                .map(Pull::head);

        final var clones = pullHead
                .flatMapMany(head -> cloneRepo
                        .findByTargetCommitSnapshotSha(head.sha()))
                .cache();

        return toResponse(clones, projectId, pullNumber);
    }

    private Mono<ServerResponse> toResponse(final Flux<Clone> clones, final long projectId, final int pullNumber) {
        class SnippetContainer {
            Snapshot snapshot;
            final List<SnippetMarker> markers = new ArrayList<>();
        }

        final var targetFileSnippets = clones
                .collect(SnippetContainer::new, (container, clone) -> {
                    container.markers.add(SnippetMarker.of(clone.targetFile(), clone.targetFromLine(), clone.targetToLine()));
                    container.snapshot = clone.targetSnapshot();
                })
                .flatMapMany(container -> codeLoader.loadSnippets(container.snapshot, container.markers));

        // TODO: Think out how to read files in a batch mode without losing an original order
        final var sourceFileSnippets = clones
                .map(clone -> Tuples.of(
                        clone.sourceSnapshot(),
                        List.of(SnippetMarker.of(clone.sourceFile(), clone.sourceFromLine(), clone.sourceToLine()))))
                .flatMapSequential(TupleUtils.function(codeLoader::loadSnippets));

        final var sourcePullNumbers = clones
                .map(clone -> Objects.requireNonNull(clone.sourceSnapshot().pullId()))
                .collectList()
                .flatMapMany(pullRepo::numbersByIds);

        final var responseClones = Flux
                .zip(clones,
                        targetFileSnippets,
                        sourceFileSnippets,
                        sourcePullNumbers)
                .map(Lambda.passingTailArgs(ClonesHandler::toCloneDto, projectId, pullNumber));

        return Responses.ok(responseClones, CloneDto.class);
    }

    private static CloneDto toCloneDto(final Clone clone,
                                       final FileEntity<Snapshot> targetFile,
                                       final FileEntity<Snapshot> sourceFile,
                                       final Integer sourcePullNumber,
                                       final long projectId,
                                       final int targetPullNumber) {
        final var target = codeSnippetWith(targetFile.ref(), Objects.requireNonNull(targetFile.content()))
                .projectId(projectId)
                .pullNumber(targetPullNumber)
                .file(clone.targetFile())
                .fromLine(clone.targetFromLine())
                .toLine(clone.targetToLine())
                .build();

        final var source = codeSnippetWith(sourceFile.ref(), Objects.requireNonNull(sourceFile.content()))
                .projectId(projectId)
                .pullNumber(sourcePullNumber)
                .file(clone.sourceFile())
                .fromLine(clone.sourceFromLine())
                .toLine(clone.sourceToLine())
                .build();

        return new CloneDto(clone.id(), target, source);
    }

    private static FlatCodeSnippetBuilder codeSnippetWith(final Snapshot snapshot, final String content) {
        return CloneDto.FlatCodeSnippet.builder()
                .owner(snapshot.repo().owner().login())
                .repo(snapshot.repo().name())
                .sha(snapshot.sha())
                .content(base64.encodeToString(content.getBytes(UTF_8)));
    }
}
