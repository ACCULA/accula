package org.accula.api.handler.exception;

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
public final class CreateProjectException extends ResponseConvertibleException {
    @Serial
    private static final long serialVersionUID = 2418056639476069599L;

    private CreateProjectException(final Error error, @Nullable final String description) {
        super(error, description);
    }

    private CreateProjectException(final Error error) {
        this(error, null);
    }

    public static CreateProjectException badFormat(final String description) {
        return new CreateProjectException(Error.BAD_FORMAT, description);
    }

    public static CreateProjectException invalidUrl() {
        return new CreateProjectException(Error.INVALID_URL);
    }

    public static CreateProjectException alreadyExists() {
        return new CreateProjectException(Error.ALREADY_EXISTS);
    }

    public static CreateProjectException wrongUrl() {
        return new CreateProjectException(Error.WRONG_URL);
    }

    public static CreateProjectException noPermission() {
        return new CreateProjectException(Error.NO_PERMISSION);
    }

    @Override
    public Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code) {
        return switch ((Error) code) {
            case BAD_FORMAT, INVALID_URL, WRONG_URL -> Responses::badRequest;
            case NO_PERMISSION -> Responses::forbidden;
            case ALREADY_EXISTS -> Responses::conflict;
        };
    }

    private enum Error implements ApiError.Code {
        BAD_FORMAT,
        INVALID_URL,
        ALREADY_EXISTS,
        WRONG_URL,
        NO_PERMISSION,
    }
}
