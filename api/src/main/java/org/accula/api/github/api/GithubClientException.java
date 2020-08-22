package org.accula.api.github.api;

import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public final class GithubClientException extends RuntimeException {
    private static final long serialVersionUID = 3511590244232877998L;

    GithubClientException(final Throwable t) {
        super(t);
    }

    static <T> Mono<T> wrap(final Throwable error) {
        return Mono.error(new GithubClientException(error));
    }
}
