package org.accula.api.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.User;
import org.accula.api.db.model.User.Role;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import static java.lang.Boolean.FALSE;

/**
 * Handles all {@code /users} routes
 *
 * @author Anton Lamtev
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class UsersHandler {
    private final CurrentUserRepo currentUserRepo;
    private final UserRepo userRepo;

    public Mono<ServerResponse> getById(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::valueOf)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest))
                .flatMap(userRepo::findById)
                .switchIfEmpty(Mono.error(Http4xxException::notFound))
                .zipWhen(this::needsRole)
                .map(TupleUtils.function(ModelToDtoConverter::convertUser))
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private Mono<Boolean> needsRole(final User user) {
        return currentUserRepo
            .get()
            .map(currentUser -> currentUser.is(Role.ROOT) || currentUser.equals(user))
            .switchIfEmpty(Mono.just(FALSE));
    }
}
