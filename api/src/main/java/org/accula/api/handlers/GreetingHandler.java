package org.accula.api.handlers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static java.util.function.Predicate.not;
import static org.springframework.web.reactive.function.server.ServerResponse.*;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;

@Component
public final class GreetingHandler {
    private static final String GREETING = "ACCULA is greeting you, ";
    private static final String ERROR = "Missing required query param \"name\"";

    @NotNull
    public Mono<ServerResponse> greet(@NotNull final ServerRequest request) {
        return Mono
                .justOrEmpty(request.queryParam("name"))
                .filter(not(String::isBlank))
                .flatMap(name -> ok().bodyValue(GREETING + name))
                .switchIfEmpty(badRequest().bodyValue(ERROR));
    }
}
