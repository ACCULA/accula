package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.DiffEntry;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handlers.dto.DiffDto;
import org.accula.api.util.Lambda;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private static final String PULL_TO_COMPARE_WITH = "with";

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
        if (!queryParams.containsKey(PULL_TO_COMPARE_WITH)) {
            return ServerResponse.badRequest().build();
        }

        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var basePullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    final var headPullNumber = Integer.parseInt(queryParams.getFirst(PULL_TO_COMPARE_WITH));

                    return diffBetweenPulls(projectId, basePullNumber, headPullNumber);
                });
    }

    private Mono<ServerResponse> diff(final long projectId, final int pullNumber) {
        final var pullMono = pullRepo
                .findByNumber(projectId, pullNumber)
                .cache();
        final var base = pullMono.map(Pull::getBase);
        final var head = pullMono.map(Pull::getHead);

        final var diff = Mono
                .zip(base, head)
                .flatMapMany(baseHead -> codeLoader.loadDiff(baseHead.getT1(), baseHead.getT2(), FileFilter.SRC_JAVA));

        return toResponse(diff);
    }

    private Mono<ServerResponse> diffBetweenPulls(final long projectId, final int basePullNumber, final int headPullNumber) {
        final var basePullSnapshot = pullRepo
                .findByNumber(projectId, basePullNumber)
                .map(Pull::getHead);
        final var headPull = pullRepo
                .findByNumber(projectId, headPullNumber)
                .cache();
        final var headPullSnapshot = headPull
                .map(Pull::getHead);
        final var projectRepo = headPull
                .map(pull -> pull.getBase().getRepo());

        final var diff = Mono
                .zip(projectRepo, basePullSnapshot, headPullSnapshot)
                .flatMapMany(Lambda.passingTailArg(codeLoader::loadRemoteDiff, FileFilter.SRC_JAVA));

        return toResponse(diff);
    }

    private static Mono<ServerResponse> toResponse(final Flux<DiffEntry> diff) {
        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(diff.map(DiffHandler::toDto), DiffDto.class);
    }

    private static DiffDto toDto(final DiffEntry diff) {
        final var base = diff.getBase();
        final var head = diff.getHead();
        return DiffDto.builder()
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
