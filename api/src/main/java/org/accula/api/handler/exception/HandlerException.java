package org.accula.api.handler.exception;

import org.accula.api.db.model.User;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.util.Responses;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.Serial;
import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public final class HandlerException extends ResponseConvertibleException {
    @Serial
    private static final long serialVersionUID = 7419056439410069539L;

    private HandlerException(final ApiError.Code code, @Nullable final String description) {
        super(code, description);
    }

    public static HandlerException badFormat(final String description) {
        return new HandlerException(Error.BAD_FORMAT, description);
    }

    public static HandlerException atLeastRoleRequired(final User.Role role) {
        return new HandlerException(Error.INSUFFICIENT_ROLE, "At least " + role + "role required");
    }

    @Override
    public Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code) {
        return switch ((Error) code) {
            case BAD_FORMAT -> Responses::badRequest;
            case INSUFFICIENT_ROLE -> Responses::forbidden;
        };
    }

    @Override
    public boolean needsResponseBody() {
        return true;
    }

    private enum Error implements ApiError.Code {
        BAD_FORMAT,
        INSUFFICIENT_ROLE,
    }
}
