package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.DiffEntry;
import org.accula.api.code.Languages;
import org.accula.api.converter.CodeToDtoConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.PathVariableExtractor;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class DiffHandler {
    private static final String PULL_TO_COMPARE_WITH = "with";

    private final PullRepo pullRepo;
    private final ProjectRepo projectRepo;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> diff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = PathVariableExtractor.projectId(request);
                    final var pullNumber = PathVariableExtractor.pullNumber(request);
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
                    final var projectId = PathVariableExtractor.projectId(request);
                    final var basePullNumber = PathVariableExtractor.pullNumber(request);
                    final var headPullNumber = Integer.valueOf(pullToCompareWith);

                    return diffBetweenPulls(projectId, basePullNumber, headPullNumber);
                })
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private Mono<ServerResponse> diff(final Long projectId, final Integer pullNumber) {
        final var pullMono = pullRepo
                .findByNumber(projectId, pullNumber)
                .switchIfEmpty(Mono.error(Http4xxException::notFound))
                .cache();
        final var base = pullMono.map(Pull::base);
        final var head = pullMono.map(Pull::head);
        final var conf = projectRepo
            .confById(projectId)
            .switchIfEmpty(Mono.error(Http4xxException::notFound));

        return Mono.zip(base, head, conf)
            .flatMapMany(TupleUtils.function(this::loadDiff))
            .as(DiffHandler::toResponse);
    }

    private Flux<DiffEntry<Snapshot>> loadDiff(final Snapshot base, final Snapshot head, final Project.Conf conf) {
        return codeLoader.loadDiff(base, head, conf.fileMinSimilarityIndex(), Languages.filter(conf.languages()));
    }

    private Mono<ServerResponse> diffBetweenPulls(final Long projectId, final Integer basePullNumber, final Integer headPullNumber) {
        final var basePullSnapshot = pullRepo
                .findByNumber(projectId, basePullNumber)
                .switchIfEmpty(Mono.error(Http4xxException::notFound))
                .map(Pull::head);
        final var headPull = pullRepo
                .findByNumber(projectId, headPullNumber)
                .switchIfEmpty(Mono.error(Http4xxException::notFound))
                .cache();
        final var headPullSnapshot = headPull
                .map(Pull::head);
        final var projectGithubRepo = headPull
                .map(pull -> pull.base().repo());
        final var conf = projectRepo
                .confById(projectId)
                .switchIfEmpty(Mono.error(Http4xxException::notFound));

        return Mono.zip(projectGithubRepo, basePullSnapshot, headPullSnapshot, conf)
            .flatMapMany(TupleUtils.function(this::loadRemoteDiff))
            .as(DiffHandler::toResponse);
    }

    private Flux<DiffEntry<Snapshot>> loadRemoteDiff(final GithubRepo repo, final Snapshot base, final Snapshot head, final Project.Conf conf) {
        return codeLoader.loadDiff(base, head, conf.fileMinSimilarityIndex(), Languages.filter(conf.languages()));
    }

    private static Mono<ServerResponse> toResponse(final Flux<DiffEntry<Snapshot>> diff) {
        return diff
                .map(CodeToDtoConverter::convert)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }
}
