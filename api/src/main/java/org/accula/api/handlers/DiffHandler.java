package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
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
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class DiffHandler {
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private static final Base64.Encoder base64 = Base64.getEncoder(); // NOPMD

    private final ProjectRepository projectRepository;
    private final CommitRepository commitRepository;
    private final PullRepository pullRepository;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> getDiff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    return getDiff(projectId, pullNumber);
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> getDiff(final long projectId, final int pullNumber) {
        final var pullMono = pullRepository
                .findByProjectIdAndNumber(projectId, pullNumber)
                .cache();
        final var base = pullMono.flatMap(pull -> projectRepository
                .findById(projectId)
                .map(project -> new Commit(-1L, project.getRepoOwner(), project.getRepoName(), pull.getBaseLastCommitSha())));

        final var head = pullMono
                .map(pull -> Mono.justOrEmpty(pull.getHeadLastCommitId()))
                .flatMap(commitRepository::findById);

        return Mono.zip(base, head)
                .flatMapMany(baseHead -> codeLoader.getDiff(baseHead.getT1(), baseHead.getT2(), FileFilter.JAVA))
                .map(DiffHandler::toResponseBody)
                .collectList()
                .flatMap(diffs -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(diffs));
    }

    private static GetDiffResponseBody toResponseBody(final Tuple2<FileEntity, FileEntity> diff) {
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
    public static String encode(@Nullable final String data) {
        if (data == null) {
            return null;
        }
        return base64.encodeToString(data.getBytes());
    }
}
