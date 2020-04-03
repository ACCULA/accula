package org.accula.api.routers;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public final class StatusRouter {
    private static final String STATUS = "{\"status\":\"ONLINE\"}";

    @Bean
    @NotNull
    public RouterFunction<ServerResponse> statusRoute() {
        final var l = new java.util.LinkedList<String>();
        for (int i = 0; i < 100; ++i) {
            l.add("" + 0);
        }
        
        for (int i = 0; i < l.size(); ++i) {
            System.out.println(l.get(i));
        }
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
