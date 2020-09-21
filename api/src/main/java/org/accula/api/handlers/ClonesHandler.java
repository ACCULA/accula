package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.SnippetMarker;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handlers.dto.CloneDto;
import org.accula.api.handlers.dto.CloneDto.FlatCodeSnippet.FlatCodeSnippetBuilder;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
                .onErrorResume(NumberFormatException.class, ClonesHandler::notFound);
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
                .onErrorResume(NumberFormatException.class, ClonesHandler::notFound);
    }

    private <T> Flux<T> doIfCurrentUserHasAdminPermissionInProject(final long projectId, final Flux<T> action) {
        return currentUserRepo
                .get(User::getId)
                .filterWhen(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId))
                .flatMapMany(currentUserId -> action);
    }

    private Mono<ServerResponse> getLastCommitClones(final long projectId, final int pullNumber) {
        final var pullHead = pullRepo
                .findByNumber(projectId, pullNumber)
                .map(Pull::getHead);

        final var clones = pullHead
                .flatMapMany(head -> cloneRepo
                        .findByTargetCommitSnapshotSha(head.getSha()))
                .cache();

        return toResponse(clones, projectId, pullNumber);
    }

    private Mono<ServerResponse> toResponse(final Flux<Clone> clones, final long projectId, final int pullNumber) {
        class SnippetContainer {
            CommitSnapshot snapshot;
            final List<SnippetMarker> markers = new ArrayList<>();
        }

        final var targetFileSnippets = clones
                .collect(SnippetContainer::new, (container, clone) -> {
                    container.markers.add(SnippetMarker.of(clone.getTargetFile(), clone.getTargetFromLine(), clone.getTargetToLine()));
                    container.snapshot = clone.getTargetSnapshot();
                })
                .flatMapMany(container -> codeLoader.loadSnippets(container.snapshot, container.markers));

        // TODO: Think out how to read files in a batch mode without losing an original order
        final var sourceFileSnippets = clones
                .map(clone -> Tuples.of(
                        clone.getSourceSnapshot(),
                        List.of(SnippetMarker.of(clone.getSourceFile(), clone.getSourceFromLine(), clone.getSourceToLine()))))
                .flatMapSequential(TupleUtils.function(codeLoader::loadSnippets));

        final var sourcePullNumbers = clones
                .map(clone -> Objects.requireNonNull(clone.getSourceSnapshot().getPullId()))
                .collectList()
                .flatMapMany(pullRepo::numbersByIds);

        final var responseClones = Flux
                .zip(clones,
                        targetFileSnippets,
                        sourceFileSnippets,
                        sourcePullNumbers)
                .map(Lambda.passingTailArgs(ClonesHandler::toCloneDto, projectId, pullNumber));

        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(responseClones, CloneDto.class);
    }

    private static CloneDto toCloneDto(final Clone clone,
                                       final FileEntity<CommitSnapshot> targetFile,
                                       final FileEntity<CommitSnapshot> sourceFile,
                                       final Integer sourcePullNumber,
                                       final long projectId,
                                       final int targetPullNumber) {
        final var target = codeSnippetWith(targetFile.getRef(), Objects.requireNonNull(targetFile.getContent()))
                .projectId(projectId)
                .pullNumber(targetPullNumber)
                .file(clone.getTargetFile())
                .fromLine(clone.getTargetFromLine())
                .toLine(clone.getTargetToLine())
                .build();

        final var source = codeSnippetWith(sourceFile.getRef(), Objects.requireNonNull(sourceFile.getContent()))
                .projectId(projectId)
                .pullNumber(sourcePullNumber)
                .file(clone.getSourceFile())
                .fromLine(clone.getSourceFromLine())
                .toLine(clone.getSourceToLine())
                .build();

        return new CloneDto(clone.getId(), target, source);
    }

    private static FlatCodeSnippetBuilder codeSnippetWith(final CommitSnapshot commitSnapshot, final String content) {
        return CloneDto.FlatCodeSnippet.builder()
                .owner(commitSnapshot.getRepo().getOwner().getLogin())
                .repo(commitSnapshot.getRepo().getName())
                .sha(commitSnapshot.getSha())
                .content(base64.encodeToString(content.getBytes(UTF_8)));
    }

    private static <E extends Throwable> Mono<ServerResponse> notFound(final E error) {
        return ServerResponse.notFound().build();
    }
}
