package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.ProjectsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
@RequiredArgsConstructor
public class ProjectsRouter {

    private final ProjectsHandler projectsHandler;

    @Bean
    public RouterFunction<ServerResponse> projectsRoute() {
        return RouterFunctions
                .route()
                .POST("/projects", projectsHandler::addProject)
                .GET("/projects", __ -> projectsHandler.getAllProjects())
                .GET("/projects/{id}", projectsHandler::getProjectById)
                .PUT("/projects/{id}", projectsHandler::updateProjectById)
                .DELETE("/projects/{id}", projectsHandler::deleteProjectById)
                .build();
    }
}
