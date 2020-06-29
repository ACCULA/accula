package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.PullRepo;
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
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class DiffHandler {
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";
    private static final String SOURCE_PULL = "source";
    private static final String TARGET_PULL = "target";

    private static final Base64.Encoder base64 = Base64.getEncoder(); // NOPMD

    private final PullRepo pullRepo;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> diff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    return diff(projectId, pullNumber);
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> diffBetweenPulls(final ServerRequest request) {
        final var queryParams = request.queryParams();
        if (!queryParams.containsKey(TARGET_PULL) || !queryParams.containsKey(SOURCE_PULL)) {
            return ServerResponse.notFound().build();
        }

        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var sourcePullNumber = Integer.parseInt(queryParams.getFirst(SOURCE_PULL));
                    final var targetPullNumber = Integer.parseInt(queryParams.getFirst(TARGET_PULL));

                    return diffBetweenPulls(projectId, sourcePullNumber, targetPullNumber);
                });
    }

    private Mono<ServerResponse> diff(final long projectId, final int pullNumber) {
        final var pullMono = pullRepo
                .findByNumber(projectId, pullNumber)
                .cache();
        final var base = pullMono.map(Pull::getBase);
        final var head = pullMono.map(Pull::getHead);

        return Mono.zip(base, head)
                .flatMapMany(baseAndHead -> codeLoader.getDiff(baseAndHead.getT1(), baseAndHead.getT2(), FileFilter.JAVA))
                .map(DiffHandler::toResponseBody)
                .collectList()
                .flatMap(diffs -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(diffs));
    }

    private Mono<ServerResponse> diffBetweenPulls(final long projectId, final int sourcePullNumber, final int targetPullNumber) {
        final var sourcePull = pullRepo
                .findByNumber(projectId, sourcePullNumber)
                .map(Pull::getHead);
        final var targetPull = pullRepo
                .findByNumber(projectId, targetPullNumber)
                .map(Pull::getHead);

        return Mono.zip(sourcePull, targetPull)
                .flatMapMany(baseAndHead -> codeLoader.getRemoteDiff(baseAndHead.getT1(), baseAndHead.getT2(), FileFilter.JAVA))
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
