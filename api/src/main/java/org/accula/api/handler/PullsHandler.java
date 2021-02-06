package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private final PullRepo pullRepo;

    //TODO: handle github errors
    public Mono<ServerResponse> getMany(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> Long.parseLong(request.pathVariable(PROJECT_ID)))
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
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));

                    return pullRepo
                            .findByNumber(projectId, pullNumber)
                            .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                            .flatMap(pull -> Mono.just(pull)
                                    .zipWith(pullRepo.findPrevious(projectId, pullNumber, pull.author().id()).collectList())
                                    .map(TupleUtils.function(ModelToDtoConverter::convert))
                                    .flatMap(Responses::ok));
                })
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .onErrorResume(Http4xxException::onErrorResume);
    }
}
