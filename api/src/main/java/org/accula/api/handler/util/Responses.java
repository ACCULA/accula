package org.accula.api.handler.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public final class Responses {
    private Responses() {
    }

    public static Mono<ServerResponse> ok() {
        return ServerResponse
                .ok()
                .build();
    }

    public static Mono<ServerResponse> ok(final Object body) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public static <T, P extends CorePublisher<? extends T>> Mono<ServerResponse> ok(final P publisher, final Class<T> clazz) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(publisher, clazz);
    }

    public static Mono<ServerResponse> created() {
        return ServerResponse
                .status(HttpStatus.CREATED)
                .build();
    }

    public static Mono<ServerResponse> created(final Object body) {
        return ServerResponse
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public static Mono<ServerResponse> accepted() {
        return ServerResponse
                .accepted()
                .build();
    }

    public static Mono<ServerResponse> notFound() {
        return ServerResponse
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    public static Mono<ServerResponse> badRequest() {
        return ServerResponse
                .badRequest()
                .build();
    }

    public static Mono<ServerResponse> badRequest(final Object body) {
        return ServerResponse
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public static Mono<ServerResponse> forbidden() {
        return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .build();
    }

    public static Mono<ServerResponse> forbidden(final Object body) {
        return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
