package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.AdminHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
@RequiredArgsConstructor
public class AdminRouter {
    private final AdminHandler handler;

    @Bean
    @NotNull
    public RouterFunction<ServerResponse> adminRoute() {
        return RouterFunctions
                .route()
                .GET("/admin", it -> handler.getAdmin())
                .build();
    }
}
