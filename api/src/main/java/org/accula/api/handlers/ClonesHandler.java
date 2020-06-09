package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.CommitOld;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.PullRepo;
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
    private final PullRepository pullRepository;
    private final CommitRepository commitRepository;
    private final CloneRepo cloneRepo;
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
        final var pullHead = pullRepo
                .findByNumber(projectId, pullNumber)
                .map(Pull::getHead)
                .cache();

//        final var clones = pullHead
//                .flatMapMany(head -> cloneRepoRepository.findAllByTargetCommitSha(head.getCommit().getSha()));

//        final var clones = pullRepository
//                .findByProjectIdAndNumber(projectId, pullNumber)
//                .flatMapMany(pull -> cloneRepo.findAllByTargetCommitId(pull.getHeadLastCommitId()))
//                .cache();
//
//        final var sourcePullNumbers = clones
//                .map(Clone::getSourceCommitSha)
//                .flatMap(pullRepository::findById)
//                .map(PullOld::getNumber);
//
//        final var commitIds = clones
//                .flatMapSequential(clone -> Flux
//                .fromIterable(List.of(clone.getSourceCommitSha(), clone.getTargetCommitSha())))
//                .distinct();
//
//        final var commits = commitRepository
//                .findAllById(commitIds)
//                .collectMap(CommitOld::getId)
//                .cache();
//
//        final var targetFileSnippetMarkers = commits
//                .flatMapMany(commitMap -> clones
//                        .map(clone -> new FileSnippetMarker(
//                                commitMap.get(clone.getTargetCommitSha()),
//                                clone.getTargetFile(),
//                                clone.getTargetFromLine(),
//                                clone.getTargetToLine())))
//                .subscribeOn(codeLoadingScheduler);
//
//        final var sourceFileSnippetMarkers = commits
//                .flatMapMany(commitMap -> clones
//                        .map(clone -> new FileSnippetMarker(
//                                commitMap.get(clone.getSourceCommitSha()),
//                                clone.getSourceFile(),
//                                clone.getSourceFromLine(),
//                                clone.getSourceToLine())))
//                .subscribeOn(codeLoadingScheduler);
//
//        return Flux
//                .zip(clones,
//                        sourcePullNumbers,
//                        getFileSnippets(targetFileSnippetMarkers),
//                        getFileSnippets(sourceFileSnippetMarkers))
//                .map(cloneAndFileEntities -> toResponseBody(cloneAndFileEntities, projectId, pullNumber))
//                .collectList()
//                .flatMap(clonesBody -> ServerResponse
//                        .ok()
//                        .contentType(APPLICATION_JSON)
//                        .bodyValue(clonesBody));
        return Mono.from(Flux.empty());
    }

    private GetCloneResponseBody toResponseBody(final Tuple4<Clone, Integer, FileEntity, FileEntity> tuple,
                                                final long projectId,
                                                final int targetPullNumber) {
//        final var clone = tuple.getT1();
//        final int sourcePullNumber = tuple.getT2();
//
//        final var targetFile = tuple.getT3();
//        final var target = codeSnippetWith(projectId, targetPullNumber, targetFile.getCommitSnapshot(), targetFile.getContent())
//                .file(clone.getTargetFile())
//                .fromLine(clone.getTargetFromLine())
//                .toLine(clone.getTargetToLine())
//                .build();
//
//        final var sourceFile = tuple.getT4();
//        final var source = codeSnippetWith(projectId, sourcePullNumber, sourceFile.getCommitSnapshot(), sourceFile.getContent())
//                .file(clone.getSourceFile())
//                .fromLine(clone.getSourceFromLine())
//                .toLine(clone.getSourceToLine())
//                .build();
//
//        return new GetCloneResponseBody(clone.getId(), target, source);
        throw new RuntimeException();
    }

    private static FlatCodeSnippetBuilder codeSnippetWith(final long projectId,
                                                          final int pullNumber,
                                                          final CommitOld commit,
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
                .flatMap(marker -> codeLoader.getFileSnippet(marker.commitSnapshot, marker.filename, marker.fromLine, marker.toLine))
                .subscribeOn(codeLoadingScheduler);
    }

    @Value
    private static class FileSnippetMarker {
        CommitSnapshot commitSnapshot;
        String filename;
        int fromLine;
        int toLine;
    }
}
