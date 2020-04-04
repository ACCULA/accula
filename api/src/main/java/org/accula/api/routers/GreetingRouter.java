package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.GreetingHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Deprecated
@Component
@RequiredArgsConstructor
public final class GreetingRouter {
    private final GreetingHandler handler;

    @Bean
    @NotNull
    public RouterFunction<ServerResponse> greetingRoute() {
        return RouterFunctions
                .route()
                .GET("/greet", handler::greet)
                .build();
    }
}
