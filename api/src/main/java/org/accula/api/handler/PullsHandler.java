package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.PathVariableExtractor;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    private final PullRepo pullRepo;

    //TODO: handle github errors
    public Mono<ServerResponse> getMany(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> PathVariableExtractor.projectId(request))
                .flatMapMany(pullRepo::findByProjectId)
                .map(ModelToDtoConverter::convertShort)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = PathVariableExtractor.projectId(request);
                    final var pullNumber = PathVariableExtractor.pullNumber(request);

                    return pullRepo
                            .findByNumber(projectId, pullNumber)
                            .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                            .flatMap(pull -> pullRepo
                                    .findPrevious(projectId, pullNumber, pull.author().id())
                                    .collectList()
                                    .map(prevPulls -> ModelToDtoConverter.convert(pull, prevPulls))
                                    .flatMap(Responses::ok));
                })
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(Http4xxException::onErrorResume);
    }
}
