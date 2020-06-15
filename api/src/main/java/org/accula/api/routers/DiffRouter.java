package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.DiffHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class DiffRouter {
    private final DiffHandler diffHandler;

    @Bean
    public RouterFunction<ServerResponse> diffRoute() {
        return RouterFunctions
                .route()
                .path("/api/projects/{projectId}/pulls", b -> b
                        .GET("/{pullNumber}/diff", diffHandler::diff)
                        .GET("/diff", diffHandler::diffBetweenPulls))
                .build();
    }
}
