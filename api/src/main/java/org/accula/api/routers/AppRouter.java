package org.accula.api.routers;

import org.accula.api.handler.util.Responses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
@Component
public final class AppRouter {
    @Value("${GITHUB_CLIENT_ID}")
    private String githubClientId;

    @Bean
    public RouterFunction<ServerResponse> appRoute() {
        return RouterFunctions
            .route()
            .path("/api/app", b -> b
                .GET("/settingsUrl", request -> Responses
                    .ok(Map.of("settingsUrl", "https://github.com/settings/connections/applications/" + githubClientId))))
            .build();
    }
}
