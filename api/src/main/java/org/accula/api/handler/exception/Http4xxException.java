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
public final class Http4xxException extends ResponseConvertibleException {
    @Serial
    private static final long serialVersionUID = -9067985327453878052L;

    public Http4xxException(final Status status, @Nullable final String message) {
        super(status, message);
    }

    public static Http4xxException badRequest() {
        return new Http4xxException(Status.BAD_REQUEST, null);
    }

    public static Http4xxException badRequest(final String description) {
        return new Http4xxException(Status.BAD_REQUEST, description);
    }

    public static Http4xxException forbidden(final String description) {
        return new Http4xxException(Status.FORBIDDEN, description);
    }

    public static Http4xxException notFound() {
        return new Http4xxException(Status.NOT_FOUND, null);
    }

    @Override
    public Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code) {
        return switch ((Status) code) {
            case BAD_REQUEST -> Responses::badRequest;
            case FORBIDDEN -> Responses::forbidden;
            case NOT_FOUND -> Responses::notFound;
        };
    }

    private enum Status implements ApiError.Code {
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND,
    }
}
