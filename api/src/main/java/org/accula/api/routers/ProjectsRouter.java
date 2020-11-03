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
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

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
                        .POST("", accept(APPLICATION_JSON), projectsHandler::create)
                        .nest(path("/{id}"), b2 -> b2
                                .GET("", projectsHandler::get)
                                .DELETE("", projectsHandler::delete)
                                .GET("/githubAdmins", projectsHandler::githubAdmins)
                                .GET("/headFiles", projectsHandler::headFiles)
                                .POST("/detectClones", projectsHandler::detectClones)
                                .nest(path("/conf"), b3 -> b3
                                        .GET("", projectsHandler::getConf)
                                        .PUT("", accept(APPLICATION_JSON), projectsHandler::updateConf))))
                .build();
    }
}
