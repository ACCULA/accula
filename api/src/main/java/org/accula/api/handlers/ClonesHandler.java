package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileSnippetMarker;
import org.accula.api.db.CloneRepository;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Commit;
import org.accula.api.handlers.response.GetCloneResponseBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

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

    public Mono<ServerResponse> getLastCommitClones(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable("projectId"));
                    final var pullNumber = Integer.parseInt(request.pathVariable("pullNumber"));

                    final var clones = pullRepo
                            .findByProjectIdAndNumber(projectId, pullNumber)
                            .flatMapMany(pull -> cloneRepo.findAllByTargetCommitId(pull.getLastCommitId()))
                            .cache();

                    final var commitIds = clones
                            .flatMapSequential(clone -> Flux
                                    .fromIterable(List.of(clone.getSourceCommitId(), clone.getTargetCommitId())));

                    final var commits = commitRepo
                            .findAllById(commitIds);

                    final var commitMapMono = commits
                            .collectMap(Commit::getId);

                    final var cloneData = commitMapMono
                            .flatMapMany(commitMap -> clones
                                    .map(clone -> Tuples.of(
                                            clone,
                                            commitMap.get(clone.getTargetCommitId()),
                                            commitMap.get(clone.getSourceCommitId()))))
                            .cache();

                    final var targetFileSnippetMarkers = commitMapMono
                            .flatMapMany(commitMap -> clones
                                    .map(clone -> {
                                        final var commit = commitMap.get(clone.getTargetCommitId());
                                        return new FileSnippetMarker(
                                                new Commit(commit.getId(), commit.getOwner(), commit.getRepo(), commit.getSha()),
                                                clone.getTargetFile(),
                                                clone.getTargetFromLine(),
                                                clone.getTargetToLine());
                                    }));

                    final var sourceFileSnippetMarkers = commitMapMono
                            .flatMapMany(commitMap -> clones
                                    .map(clone -> {
                                        final var commit = commitMap.get(clone.getSourceCommitId());
                                        return new FileSnippetMarker(
                                                new Commit(commit.getId(), commit.getOwner(), commit.getRepo(), commit.getSha()),
                                                clone.getSourceFile(),
                                                clone.getSourceFromLine(),
                                                clone.getSourceToLine());
                                    }));

                    return Flux
                            .zip(cloneData,
                                    codeLoader.getFileSnippets(targetFileSnippetMarkers),
                                    codeLoader.getFileSnippets(sourceFileSnippetMarkers))
                            .map(tuple -> {
                                final var cloneDatum = tuple.getT1();
                                final var clone = cloneDatum.getT1();

                                final var targetCommit = cloneDatum.getT2();
                                final var targetFileContent = tuple.getT2();
                                final var target = GetCloneResponseBody.FlatCodeSnippet
                                        .builder()
                                        .projectId(projectId)
                                        .pullNumber(pullNumber)
                                        .sha(targetCommit.getSha())
                                        .file(clone.getTargetFile())
                                        .fromLine(clone.getTargetFromLine())
                                        .toLine(clone.getTargetToLine())
                                        .content(targetFileContent)
                                        .build();

                                final var sourceCommit = cloneDatum.getT3();
                                final var sourceFileContent = tuple.getT3();
                                final var source = GetCloneResponseBody.FlatCodeSnippet
                                        .builder()
                                        .projectId(projectId)
                                        .pullNumber(pullNumber)
                                        .sha(sourceCommit.getSha())
                                        .file(clone.getSourceFile())
                                        .fromLine(clone.getSourceFromLine())
                                        .toLine(clone.getSourceToLine())
                                        .content(sourceFileContent)
                                        .build();

                                return new GetCloneResponseBody(clone.getId(), target, source);
                            })
                            .collectList()
                            .flatMap(body -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(body));
                });
    }
}
