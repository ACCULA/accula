package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handler.AppHandler;
import org.accula.api.handler.util.Responses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class AppRouter {
    private final AppHandler appHandler;
    @Value("${GITHUB_CLIENT_ID}")
    private String githubClientId;

    @Bean
    public RouterFunction<ServerResponse> appRoute() {
        return RouterFunctions
            .route()
            .path("/api/app", b -> b
                .GET("/settingsUrl", request -> Responses
                    .ok(Map.of("settingsUrl", "https://github.com/settings/connections/applications/" + githubClientId)))
                .path("/settings", b1 -> b1
                    .GET("", appHandler::settings)
                    .PUT("", accept(APPLICATION_JSON), appHandler::updateSettings)))
            .build();
    }
}
