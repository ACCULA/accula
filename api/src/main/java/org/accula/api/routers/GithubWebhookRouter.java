package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.GithubWebhookHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class GithubWebhookRouter {
    private final GithubWebhookHandler webhookHandler;

    @Bean
    public RouterFunction<ServerResponse> webhookRoute() {
        return RouterFunctions
                .route(POST("/api/webhook"), webhookHandler::webhook);
    }
}
