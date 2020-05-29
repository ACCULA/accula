package org.accula.api.routers;

import lombok.RequiredArgsConstructor;
import org.accula.api.handlers.ClonesHandler;
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
                .GET("/projects/{projectId}/pulls/{pullId}/clones/", clonesHandler::getLastCommitClones)
                .build();
    }
}
