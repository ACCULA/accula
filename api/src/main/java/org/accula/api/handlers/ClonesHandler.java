package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple4;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
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

    private final Scheduler codeLoadingScheduler = Schedulers.boundedElastic();
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

                    return doIfCurrentUserHasAdminPermissionInProject(projectId, cloneRepo
                            .deleteByPullNumber(projectId, pullNumber)
                            .then(pullRepo
                                    .findByNumber(projectId, pullNumber)
                                    .flatMapMany(cloneDetectionService::detectClones)
                                    .then())
                            .then(getLastCommitClones(projectId, pullNumber)))
                            .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(NumberFormatException.class, ClonesHandler::notFound);
    }

    private <T> Mono<T> doIfCurrentUserHasAdminPermissionInProject(final long projectId, final Mono<T> action) {
        return currentUserRepo
                .get()
                .map(User::getId)
                .filterWhen(currentUserId -> projectRepo.hasCreatorOrAdminWithId(projectId, currentUserId))
                .flatMap(currentUserId -> action);
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
        final var targetFileSnippets = clones
                .collectList()
                .flatMapMany(cloneList -> Flux.defer(() -> {
                    final var commitSnapshot = cloneList.get(0).getTargetSnapshot();
                    final var snippetMarkers = cloneList
                            .stream()
                            .map(clone -> SnippetMarker.of(clone.getTargetFile(), clone.getTargetFromLine(), clone.getTargetToLine()))
                            .collect(toList());
                    return codeLoader.loadSnippets(commitSnapshot, snippetMarkers);
                }));

        final var sourcePullNumbers = clones
                .map(clone -> Objects.requireNonNull(clone.getSourceSnapshot().getPullId()))
                .collectList()
                .flatMapMany(pullRepo::numbersByIds);

        final var sourceFileSnippetMarkers = clones
                .map(clone -> new FileSnippetMarker(
                        clone.getSourceSnapshot(),
                        clone.getSourceFile(),
                        clone.getSourceFromLine(),
                        clone.getSourceToLine()
                ));

        final var responseClones = Flux
                .zip(clones,
                        targetFileSnippets,
                        getFileSnippets(sourceFileSnippetMarkers),
                        sourcePullNumbers)
                .map(tuple -> toCloneDto(tuple, projectId, pullNumber));

        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(responseClones, CloneDto.class);
    }

    private CloneDto toCloneDto(final Tuple4<Clone, FileEntity, FileEntity, Integer> tuple,
                                final long projectId,
                                final int targetPullNumber) {
        final var clone = tuple.getT1();

        final var targetFile = tuple.getT2();
        final var target = codeSnippetWith(targetFile.getCommitSnapshot(), Objects.requireNonNull(targetFile.getContent()))
                .projectId(projectId)
                .pullNumber(targetPullNumber)
                .file(clone.getTargetFile())
                .fromLine(clone.getTargetFromLine())
                .toLine(clone.getTargetToLine())
                .build();

        final var sourceFile = tuple.getT3();
        final var sourcePullNumber = tuple.getT4();
        final var source = codeSnippetWith(sourceFile.getCommitSnapshot(), Objects.requireNonNull(sourceFile.getContent()))
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

    private Flux<FileEntity> getFileSnippets(final Flux<FileSnippetMarker> markers) {
        return markers
                .flatMapSequential(marker -> codeLoader
                        //FIXME: The Current solution is an adaptor of the previous one using new api
                        //TODO: We need to load snippets in a batch
                        .loadSnippets(marker.commitSnapshot, List.of(SnippetMarker.of(marker.filename, marker.fromLine, marker.toLine))))
                .subscribeOn(codeLoadingScheduler);
    }

    private static <E extends Throwable> Mono<ServerResponse> notFound(final E ignored) {
        return ServerResponse.notFound().build();
    }

    @Value
    private static class FileSnippetMarker {
        CommitSnapshot commitSnapshot;
        String filename;
        int fromLine;
        int toLine;
    }
}
