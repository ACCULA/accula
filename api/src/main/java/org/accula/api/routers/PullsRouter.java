package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.PullsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsRouter {
    private final PullsHandler pullsHandler;

    @Bean
    public RouterFunction<ServerResponse> pullsRoute() {
        return RouterFunctions
                .route()
                .path("/api/projects/{projectId}/pulls", builder -> builder
                        .GET("", pullsHandler::getAll)
                        .GET("/{pullNumber}", pullsHandler::get)
                        .POST("/refresh", pullsHandler::refresh))
                .build();
    }
}
