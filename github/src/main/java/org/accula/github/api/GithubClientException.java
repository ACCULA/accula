package org.accula.github.api;

/**
 * @author Anton Lamtev
 */
public final class GithubClientException extends RuntimeException {
    private static final long serialVersionUID = 3511590244232877998L;

    GithubClientException(final Throwable t) {
        super(t);
    }
}
