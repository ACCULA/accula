package org.accula.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class StatusHandler {
    private static final String STATUS = "{\"status\":\"ONLINE\"}";

    public Mono<ServerResponse> getStatus() {
        return ok()
                .contentType(APPLICATION_JSON)
                .bodyValue(STATUS);
    }
}
