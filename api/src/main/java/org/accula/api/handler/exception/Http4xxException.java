package org.accula.api.handler.exception;

import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.util.Responses;
import org.accula.api.util.Lambda;
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

    public Http4xxException(final Status status) {
        super(status, null);
    }

    public static Http4xxException badRequest() {
        return new Http4xxException(Status.BAD_REQUEST);
    }

    public static Http4xxException forbidden() {
        return new Http4xxException(Status.FORBIDDEN);
    }

    public static Http4xxException notFound() {
        return new Http4xxException(Status.NOT_FOUND);
    }

    @Override
    public Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code) {
        return Lambda.expandingWithArg((switch ((Status) code) {
            case BAD_REQUEST -> Responses::badRequest;
            case FORBIDDEN -> Responses::forbidden;
            case NOT_FOUND -> Responses::notFound;
        }));
    }

    @Override
    public boolean needsResponseBody() {
        return false;
    }

    private enum Status implements ApiError.Code {
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND,
    }
}
