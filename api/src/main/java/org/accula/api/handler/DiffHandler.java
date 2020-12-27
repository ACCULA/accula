package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.DiffEntry;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.dto.DiffDto;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class DiffHandler {
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";
    private static final String PULL_TO_COMPARE_WITH = "with";

    private static final Base64.Encoder base64 = Base64.getEncoder(); // NOPMD

    private final PullRepo pullRepo;
    private final ProjectRepo projectRepo;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> diff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    return diff(projectId, pullNumber);
                })
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> diffBetweenPulls(final ServerRequest request) {
        final var queryParams = request.queryParams();
        final var pullToCompareWith = queryParams.getFirst(PULL_TO_COMPARE_WITH);
        if (pullToCompareWith == null) {
            return Responses.badRequest();
        }

        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var basePullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));
                    final var headPullNumber = Integer.parseInt(pullToCompareWith);

                    return diffBetweenPulls(projectId, basePullNumber, headPullNumber);
                })
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private Mono<ServerResponse> diff(final long projectId, final int pullNumber) {
        final var pullMono = pullRepo
                .findByNumber(projectId, pullNumber)
                .cache();
        final var base = pullMono.map(Pull::getBase);
        final var head = pullMono.map(Pull::getHead);
        final var minSimilarityIndex = projectRepo
                .confById(projectId)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(Project.Conf::getFileMinSimilarityIndex);

        final var diff = Mono
                .zip(base, head, minSimilarityIndex)
                .flatMapMany(t -> codeLoader.loadDiff(t.getT1(), t.getT2(), t.getT3(), FileFilter.SRC_JAVA));

        return toResponse(diff);
    }

    private Mono<ServerResponse> diffBetweenPulls(final long projectId, final int basePullNumber, final int headPullNumber) {
        final var basePullSnapshot = pullRepo
                .findByNumber(projectId, basePullNumber)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(Pull::getHead);
        final var headPull = pullRepo
                .findByNumber(projectId, headPullNumber)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .cache();
        final var headPullSnapshot = headPull
                .map(Pull::getHead);
        final var projectGithubRepo = headPull
                .map(pull -> pull.getBase().getRepo());
        final var minSimilarityIndex = projectRepo
                .confById(projectId)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(Project.Conf::getFileMinSimilarityIndex);

        final var diff = Mono
                .zip(projectGithubRepo, basePullSnapshot, headPullSnapshot, minSimilarityIndex)
                .flatMapMany(t -> codeLoader.loadRemoteDiff(t.getT1(), t.getT2(), t.getT3(), t.getT4(), FileFilter.SRC_JAVA));

        return toResponse(diff);
    }

    private static Mono<ServerResponse> toResponse(final Flux<DiffEntry<Snapshot>> diff) {
        return Responses.ok(diff.map(DiffHandler::toDto), DiffDto.class);
    }

    private static DiffDto toDto(final DiffEntry<Snapshot> diff) {
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
