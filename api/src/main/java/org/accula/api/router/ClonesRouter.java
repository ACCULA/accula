package org.accula.api.router;

import lombok.RequiredArgsConstructor;
import org.accula.api.handler.ClonesHandler;
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
public final class ClonesRouter {
    private final ClonesHandler clonesHandler;

    @Bean
    public RouterFunction<ServerResponse> clonesRoute() {
        return RouterFunctions
                .route()
                .path("/api/projects/{projectId}/pulls/{pullNumber}/clones", b -> b
                        .GET("", clonesHandler::getLastCommitClones)
                        .POST("/refresh", clonesHandler::refreshClones))
                .build();
    }
}
