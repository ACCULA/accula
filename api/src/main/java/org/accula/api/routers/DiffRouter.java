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
                .GET("/api/projects/{projectId}/pulls/{pullNumber}/diff", diffHandler::getDiff)
                .build();
    }
}
