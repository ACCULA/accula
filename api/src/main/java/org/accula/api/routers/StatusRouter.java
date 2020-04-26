package org.accula.api.routers;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public final class StatusRouter {
    private static final String STATUS = "{\"status\":\"ONLINE\"}";

    @Bean
    public RouterFunction<ServerResponse> statusRoute() {
        return RouterFunctions
                .route()
                .GET("/status", __ ->
                        ServerResponse
                                .ok()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(STATUS))
                .build();
    }
}
