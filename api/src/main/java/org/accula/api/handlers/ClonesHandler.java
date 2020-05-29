package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
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

                    return commitRepo
                            .findAllById(commitIds)
                            .collectMap(Commit::getId)
                            .flatMapMany(commitMap -> clones
                                    .map(clone -> {
                                        final var target = GetCloneResponseBody.FlatCodeSnippet
                                                .builder()
                                                .projectId(projectId)
                                                .pullNumber(pullNumber)
                                                .sha(commitMap.get(clone.getTargetCommitId()).getSha())
                                                .file(clone.getTargetFile())
                                                .fromLine(clone.getTargetFromLine())
                                                .toLine(clone.getTargetToLine())
                                                .content("")
                                                .build();

                                        final var source = GetCloneResponseBody.FlatCodeSnippet
                                                .builder()
                                                .projectId(projectId)
                                                .pullNumber(pullNumber)
                                                .sha(commitMap.get(clone.getSourceCommitId()).getSha())
                                                .file(clone.getSourceFile())
                                                .fromLine(clone.getSourceFromLine())
                                                .toLine(clone.getSourceToLine())
                                                .content("")
                                                .build();

                                        return new GetCloneResponseBody(clone.getId(), target, source);
                                    }))
                            .collectList()
                            .flatMap(body -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(body));
                });
    }
}
