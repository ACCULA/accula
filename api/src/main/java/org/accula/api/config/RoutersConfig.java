package org.accula.api.config;

import org.accula.api.handlers.AdminHandler;
import org.accula.api.handlers.GreetingHandler;
import org.accula.api.handlers.StatusHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RoutersConfig {

    @Bean
    public RouterFunction<ServerResponse> adminRoute(AdminHandler adminHandler) {
        return RouterFunctions
                .route()
                .GET("/admin", __ -> adminHandler.getAdmin())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> greetingRoute(GreetingHandler greetingHandler) {
        return RouterFunctions
                .route()
                .GET("/greet", greetingHandler::greet)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> statusRoute(StatusHandler statusHandler) {
        return RouterFunctions
                .route()
                .GET("/status", __ -> statusHandler.getStatus())
                .build();
    }
}
