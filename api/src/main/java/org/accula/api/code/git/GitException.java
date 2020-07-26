package org.accula.api.code.git;

/**
 * @author Anton Lamtev
 */
public final class GitException extends RuntimeException {
    public GitException(final Throwable e) {
        super(e);
    }
}
