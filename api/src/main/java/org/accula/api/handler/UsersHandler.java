package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
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
    private final UserRepo userRepo;

    public Mono<ServerResponse> getById(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::valueOf)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .flatMap(userRepo::findById)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(ModelToDtoConverter::convert)
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }
}
