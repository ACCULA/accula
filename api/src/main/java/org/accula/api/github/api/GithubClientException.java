package org.accula.api.github.api;

import java.io.Serial;

/**
 * @author Anton Lamtev
 */
public final class GithubClientException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3511590244232877998L;

    GithubClientException(final Throwable t) {
        super(t);
    }
}
