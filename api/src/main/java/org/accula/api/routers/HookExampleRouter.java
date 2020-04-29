package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.HookExampleHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Component
@RequiredArgsConstructor
public final class HookExampleRouter {
    private final HookExampleHandler handler;

    @Bean
    public RouterFunction<ServerResponse> hookRoute() {
        return RouterFunctions
                .route()
                .POST("/hook", accept(APPLICATION_JSON), handler::process)
                .build();
    }
}
