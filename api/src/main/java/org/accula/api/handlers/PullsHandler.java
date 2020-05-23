package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.github.api.GithubClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    private final ProjectRepository projects;
    private final PullRepository pulls;
    private final GithubClient githubClient;

    public Mono<ServerResponse> getAll(final ServerRequest request) {
        return Mono.empty();
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono.empty();
    }

    public Mono<ServerResponse>refresh(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("pullNumber"))
                //switchIfEmpty
                .map(Long::parseLong)
                //onError
                .flatMap(projects::findById)
                .then(Mono.empty());
    }
}
