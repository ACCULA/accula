package org.accula.api.handler.exception;

import org.accula.api.handler.dto.ApiError;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.Serial;
import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public abstract class ResponseConvertibleException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 9047541397182907053L;

    private final ApiError.Code code;

    public ResponseConvertibleException(final ApiError.Code code, @Nullable final String description) {
        super(description);
        this.code = code;
    }

    public static Mono<ServerResponse> onErrorResume(final Throwable e) {
        if (!(e instanceof ResponseConvertibleException responseConvertible)) {
            return Mono.error(e);
        }
        return responseConvertible.toResponse();
    }

    public abstract Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code);

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    private Mono<ServerResponse> toResponse() {
        final var apiError = toApiError();
        return responseFunctionForCode(code).apply(apiError);
    }

    private ApiError toApiError() {
        final var description = getMessage();
        if (description == null) {
            return ApiError.with(code);
        }
        return ApiError.withDescription(description, code);
    }
}
