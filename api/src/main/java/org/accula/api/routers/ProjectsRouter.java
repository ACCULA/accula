package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.ProjectsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ProjectsRouter {
    private final ProjectsHandler projectsHandler;

    @Bean
    public RouterFunction<ServerResponse> projectsRoute() {
        return RouterFunctions
                .route()
                .path("/api/projects", b1 -> b1
                        .GET("", projectsHandler::getTop)
                        .GET("/{id}", projectsHandler::get)
                        .nest(accept(APPLICATION_JSON), b2 -> b2
                                .POST("", projectsHandler::create)
                                .DELETE("/{id}", projectsHandler::delete)))
                .build();
    }
}
