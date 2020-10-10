package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handlers.util.Responses;
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
    //TODO: common handler for all NOT FOUND cases
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception("PULL_NOT_FOUND_EXCEPTION");
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private final PullRepo pullRepo;

    //TODO: handle github errors
    public Mono<ServerResponse> getMany(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> Long.parseLong(request.pathVariable(PROJECT_ID)))
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .flatMapMany(pullRepo::findByProjectId)
                .map(ModelToDtoConverter::convertShort)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound));
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable(PROJECT_ID));
                    final var pullNumber = Integer.parseInt(request.pathVariable(PULL_NUMBER));

                    return pullRepo
                            .findByNumber(projectId, pullNumber)
                            .switchIfEmpty(Mono.error(PULL_NOT_FOUND_EXCEPTION))
                            .flatMap(pull -> Mono.just(pull)
                                    .zipWith(pullRepo.findPrevious(projectId, pullNumber, pull.getAuthor().getId()).collectList())
                                    .map(TupleUtils.function(ModelToDtoConverter::convert))
                                    .flatMap(Responses::ok));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound));
    }
}
