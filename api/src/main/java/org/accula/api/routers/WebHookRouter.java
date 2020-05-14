package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.WebHookHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
@RequiredArgsConstructor
public class WebHookRouter {
    private final WebHookHandler handler;

    @Bean
    public RouterFunction<ServerResponse> dataParserRoute() {
        return RouterFunctions
                .route()
                .POST("/data", handler::getWebHookInformation)
                .build();
    }
}
