package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Commit;
import org.accula.api.handlers.response.GetDiffResponseBody;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Base64;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class DiffHandler {
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private final Base64.Encoder base64 = Base64.getEncoder();

    private final ProjectRepository projectRepository;
    private final CommitRepository commitRepository;
    private final PullRepository pullRepository;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> getDiff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    final var baseSha = request.queryParam("sha").orElseThrow();
                    final var base = projectRepository.findById(projectId)
                            .map(p -> new Commit(-1L, p.getRepoOwner(), p.getRepoName(), baseSha));

                    final var head = pullRepository.findByProjectIdAndNumber(projectId, pullNumber)
                            .map(pull -> Mono.justOrEmpty(pull.getLastCommitId()))
                            .flatMap(commitRepository::findById);

                    return Mono.zip(base, head)
                            .flatMapMany(headBase -> codeLoader.getDiff(headBase.getT1(), headBase.getT2()))
                            .map(this::toResponseBody)
                            .collectList()
                            .flatMap(diffs -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(diffs));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(e -> e == PULL_NOT_FOUND_EXCEPTION, e -> ServerResponse.notFound().build());
    }

    private GetDiffResponseBody toResponseBody(final Tuple2<FileEntity, FileEntity> diff) {
        final var base = diff.getT1();
        final var head = diff.getT2();
        return GetDiffResponseBody.builder()
                .baseFilename(base.getName())
                .baseContent(encode(base.getContent()))
                .headFilename(head.getName())
                .headContent(encode(head.getContent()))
                .build();
    }

    @Nullable
    public String encode(@Nullable final String data) {
        if (data == null) {
            return null;
        }
        return base64.encodeToString(data.getBytes());
    }
}
