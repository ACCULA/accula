package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handlers.dto.ShortPullDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    //TODO: common handler for all NOT FOUND cases
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private final PullRepo pullRepo;

    //TODO: handle github errors
    public Mono<ServerResponse> getMany(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> Long.parseLong(request.pathVariable(PROJECT_ID)))
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .flatMap(projectId -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .body(pullRepo.findByProjectId(projectId).map(ModelToDtoConverter::convertShort), ShortPullDto.class))
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
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
                                    .flatMap(pullWithPrev -> ServerResponse
                                            .ok()
                                            .contentType(APPLICATION_JSON)
                                            .bodyValue(ModelToDtoConverter.convert(pullWithPrev.getT1(), pullWithPrev.getT2()))));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(PULL_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }
}
