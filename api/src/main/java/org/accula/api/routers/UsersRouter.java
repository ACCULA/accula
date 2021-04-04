package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handler.UsersHandler;
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
public final class UsersRouter {
    private final UsersHandler usersHandler;

    @Bean
    public RouterFunction<ServerResponse> usersRoute() {
        return RouterFunctions
                .route()
                .GET("/api/users/{id}", usersHandler::getById)
                .build();
    }
}
