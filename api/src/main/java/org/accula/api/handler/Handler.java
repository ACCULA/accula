package org.accula.api.handler;

import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.handler.dto.InputDto;
import org.accula.api.handler.dto.validation.Errors;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.HandlerException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public interface Handler {
    InputDtoValidator validator();

    CurrentUserRepo currentUserRepo();

    default Mono<User> checkAdminRole() {
        return checkRole(User.Role.ADMIN);
    }

    default Mono<User> checkRootRole() {
        return checkRole(User.Role.ROOT);
    }

    default void validate(final InputDto dto) {
        validate(dto, HandlerException::badFormat, "Bad format: ");
    }

    default void validate(final InputDto object,
                          final Function<String, ResponseConvertibleException> exceptionFactory,
                          final String... exceptionMessagePrefix) {
        final var errors = new Errors(object, object.getClass().getSimpleName());
        validator().validate(object, errors);
        if (errors.hasErrors()) {
            final var prefix = exceptionMessagePrefix.length == 1 ? exceptionMessagePrefix[0] : "";
            throw exceptionFactory.apply(prefix + errors.joinedDescription());
        }
    }

    private Mono<User> checkRole(final User.Role role) {
        return currentUserRepo()
            .get()
            .handle((user, sink) -> {
                if (user.role().compareTo(role) < 0) {
                    sink.error(HandlerException.atLeastRoleRequired(role));
                    return;
                }
                sink.next(user);
                sink.complete();
            });
    }
}
