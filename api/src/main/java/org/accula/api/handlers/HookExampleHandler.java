package org.accula.api.handlers;

import lombok.Data;
import org.accula.core.Analyzer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public final class HookExampleHandler {

    @NotNull
    public Mono<ServerResponse> process(@NotNull final ServerRequest request) {
        return request.bodyToMono(HookRequest.class)
                .doOnNext(body -> {
                    var analyzer = new Analyzer(body.getSource(),
                            body.getToken(),
                            body.getInclude(),
                            body.getExclude(),
                            body.getThreshold());
                    analyzer.analyze();
                })
                .flatMap(__ -> ok().bodyValue("OK"))
                .switchIfEmpty(badRequest().bodyValue("Error"));
    }

    @Data
    private static class HookRequest {
        private String source;
        private String token;
        private List<String> include;
        private List<String> exclude;
        private float threshold;
    }
}
