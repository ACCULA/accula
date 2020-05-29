package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.db.CloneRepository;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;
import org.accula.api.handlers.response.GetCloneResponseBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ClonesHandler {
    private final PullRepository pullRepo;
    private final CommitRepository commitRepo;
    private final CloneRepository cloneRepo;
    private final CodeLoader codeLoader;
    private final Scheduler codeLoadingScheduler = Schedulers.boundedElastic();

    public Mono<ServerResponse> getLastCommitClones(final ServerRequest request) {
        return Mono.defer(() -> {
            final var projectId = Long.parseLong(request.pathVariable("projectId"));
            final var pullNumber = Integer.parseInt(request.pathVariable("pullNumber"));
            return getLastCommitClones(projectId, pullNumber);
        });
    }

    private Mono<ServerResponse> getLastCommitClones(final long projectId, final int pullNumber) {
        final var clones = pullRepo
                .findByProjectIdAndNumber(projectId, pullNumber)
                .flatMapMany(pull -> cloneRepo.findAllByTargetCommitId(pull.getLastCommitId()))
                .cache();

        final var commitIds = clones.flatMapSequential(clone -> Flux
                .fromIterable(List.of(clone.getSourceCommitId(), clone.getTargetCommitId())));

        final var commits = commitRepo.findAllById(commitIds);
        final var commitMapMono = commits.collectMap(Commit::getId);

        final var targetFileSnippetMarkers = commitMapMono
                .flatMapMany(commitMap -> clones
                        .map(clone -> new FileSnippetMarker(
                                commitMap.get(clone.getTargetCommitId()),
                                clone.getTargetFile(),
                                clone.getTargetFromLine(),
                                clone.getTargetToLine())));

        final var sourceFileSnippetMarkers = commitMapMono
                .flatMapMany(commitMap -> clones
                        .map(clone -> new FileSnippetMarker(
                                commitMap.get(clone.getSourceCommitId()),
                                clone.getSourceFile(),
                                clone.getSourceFromLine(),
                                clone.getSourceToLine())));

        final var cloneData = commitMapMono
                .flatMapMany(commitMap -> clones);

        return Flux
                .zip(cloneData,
                        getFileSnippets(targetFileSnippetMarkers),
                        getFileSnippets(sourceFileSnippetMarkers))
                .map(tuple -> toResponseBody(tuple, projectId, pullNumber))
                .collectList()
                .flatMap(clonesBody -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(clonesBody));
    }

    private GetCloneResponseBody toResponseBody(final Tuple3<Clone, FileEntity, FileEntity> tuple,
                                                final long projectId,
                                                final int pullNumber) {
        final var clone = tuple.getT1();

        final var targetFile = tuple.getT2();
        final var target = GetCloneResponseBody.FlatCodeSnippet.builder()
                .projectId(projectId)
                .pullNumber(pullNumber)
                .sha(targetFile.getCommit().getSha())
                .content(targetFile.getContent())
                .file(clone.getTargetFile())
                .fromLine(clone.getTargetFromLine())
                .toLine(clone.getTargetToLine())
                .build();

        final var sourceFile = tuple.getT3();
        final var source = GetCloneResponseBody.FlatCodeSnippet.builder()
                .projectId(projectId)
                .pullNumber(pullNumber)
                .sha(sourceFile.getCommit().getSha())
                .content(sourceFile.getContent())
                .file(clone.getSourceFile())
                .fromLine(clone.getSourceFromLine())
                .toLine(clone.getSourceToLine())
                .build();

        return new GetCloneResponseBody(clone.getId(), target, source);
    }

    private Flux<FileEntity> getFileSnippets(final Flux<FileSnippetMarker> markers) {
        return markers
                .flatMap(marker -> codeLoader.getFileSnippet(marker.commit, marker.filename, marker.fromLine, marker.toLine))
                .subscribeOn(codeLoadingScheduler);
    }

    @Value
    private static class FileSnippetMarker {
        Commit commit;
        String filename;
        int fromLine;
        int toLine;
    }
}
