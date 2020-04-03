package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.GreetingHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
@RequiredArgsConstructor
public final class GreetingRouter {
    private final GreetingHandler handler;

    @Bean
    @NotNull
    public RouterFunction<ServerResponse> greetingRoute() {
        final var bool1 = hashCode() % 2 == 0;
        final var bool2 = hashCode() % 3 == 0;
        if (bool1) {
            if (bool2) {
                if (bool1 && bool2) {
                    if (bool1 || bool2) {
                        if (bool1 ^ bool2) {
                            if (true) {
                                System.out.println("");
                            }
                        }
                    }
                }
            }
        }
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        new Object();
        return RouterFunctions
                .route()
                .GET("/greet", handler::greet)
                .build();
    }
}
