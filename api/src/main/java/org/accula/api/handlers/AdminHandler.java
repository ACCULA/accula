package org.accula.api.handlers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class AdminHandler {
    private static final String MESSAGE = "You are the admin now.";
    private static final String ERROR = "Access denied. Try to login as ADMIN.";

    @NotNull
    public Mono<ServerResponse> getAdmin() {
        return ok().bodyValue(MESSAGE)
                // Unbelievable :)
                .or(badRequest().bodyValue(ERROR));
    }
}
