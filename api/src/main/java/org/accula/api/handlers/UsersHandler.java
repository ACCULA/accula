package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handlers.util.Responses;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handles all {@code /users} routes
 *
 * @author Anton Lamtev
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class UsersHandler {
    //TODO: common handler for all NOT FOUND cases
    private static final Exception USER_NOT_FOUND_EXCEPTION = new Exception("USER_NOT_FOUND_EXCEPTION");

    private final UserRepo userRepo;

    public Mono<ServerResponse> getById(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(NumberFormatException.class, e -> USER_NOT_FOUND_EXCEPTION)
                .flatMap(userRepo::findById)
                .map(ModelToDtoConverter::convert)
                .flatMap(Responses::ok)
                .switchIfEmpty(Mono.error(USER_NOT_FOUND_EXCEPTION))
                .onErrorResume(USER_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound));
    }
}
