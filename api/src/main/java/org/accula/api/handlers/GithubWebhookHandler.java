package org.accula.api.handlers;

import org.accula.api.github.model.GithubHookPayload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Component
public final class GithubWebhookHandler {
    public Mono<ServerResponse> webhook(final ServerRequest request) {
        return request
                .bodyToMono(GithubHookPayload.class)
                .flatMap(p -> Mono.empty());
    }
}
