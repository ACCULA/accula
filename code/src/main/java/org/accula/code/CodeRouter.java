package org.accula.code;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
@RequiredArgsConstructor
public final class CodeRouter {
    private final CodeHandler handler;

    @Bean
    public RouterFunction<ServerResponse> greetingRoute() {
        return RouterFunctions
                .route()
                .GET("/{owner}/{repo}/{sha}/**", handler::getFile)
                .build();
    }
}
