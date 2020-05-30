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
import org.accula.api.db.model.Pull;
import org.accula.api.handlers.response.GetCloneResponseBody;
import org.accula.api.handlers.response.GetCloneResponseBody.FlatCodeSnippet.FlatCodeSnippetBuilder;
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

    private final PullRepository pullRepo;
    private final CommitRepository commitRepo;
    private final CloneRepository cloneRepo;
    private final CodeLoader codeLoader;
    private final Scheduler codeLoadingScheduler = Schedulers.boundedElastic();

    public Mono<ServerResponse> getLastCommitClones(final ServerRequest request) {
        return Mono.defer(() -> {
            final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
            final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
            return getLastCommitClones(projectId, pullNumber);
        });
    }

    private Mono<ServerResponse> getLastCommitClones(final long projectId, final int pullNumber) {
        final var clones = pullRepo
                .findByProjectIdAndNumber(projectId, pullNumber)
                .flatMapMany(pull -> cloneRepo.findAllByTargetCommitId(pull.getHeadLastCommitId()))
                .cache();

        final var sourcePullNumbers = pullRepo
                .findAllById(clones.map(Clone::getSourceCommitId).distinct())
                .map(Pull::getNumber);

        final var commitIds = clones.flatMapSequential(clone -> Flux
                .fromIterable(List.of(clone.getSourceCommitId(), clone.getTargetCommitId())))
                .distinct();

        final var commits = commitRepo.findAllById(commitIds).cache();
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

        return Flux
                .zip(clones,
                        sourcePullNumbers,
                        getFileSnippets(targetFileSnippetMarkers),
                        getFileSnippets(sourceFileSnippetMarkers))
                .map(cloneAndFileEntities -> toResponseBody(cloneAndFileEntities, projectId, pullNumber))
                .collectList()
                .flatMap(clonesBody -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(clonesBody));
    }

    private GetCloneResponseBody toResponseBody(final Tuple4<Clone, Integer, FileEntity, FileEntity> tuple,
                                                final long projectId,
                                                final int targetPullNumber) {
        final var clone = tuple.getT1();
        final int sourcePullNumber = tuple.getT2();

        final var targetFile = tuple.getT3();
        final var target = codeSnippetWith(projectId, targetPullNumber, targetFile.getCommit(), targetFile.getContent())
                .file(clone.getTargetFile())
                .fromLine(clone.getTargetFromLine())
                .toLine(clone.getTargetToLine())
                .build();

        final var sourceFile = tuple.getT4();
        final var source = codeSnippetWith(projectId, sourcePullNumber, sourceFile.getCommit(), sourceFile.getContent())
                .file(clone.getSourceFile())
                .fromLine(clone.getSourceFromLine())
                .toLine(clone.getSourceToLine())
                .build();

        return new GetCloneResponseBody(clone.getId(), target, source);
    }

    private static FlatCodeSnippetBuilder codeSnippetWith(final long projectId,
                                                          final int pullNumber,
                                                          final Commit commit,
                                                          final String content) {
        return GetCloneResponseBody.FlatCodeSnippet.builder()
                .projectId(projectId)
                .pullNumber(pullNumber)
                .owner(commit.getOwner())
                .repo(commit.getRepo())
                .sha(commit.getSha())
                .content(base64.encodeToString(content.getBytes()));
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
