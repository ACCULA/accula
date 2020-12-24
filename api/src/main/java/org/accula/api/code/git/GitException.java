package org.accula.api.code.git;

import java.io.Serial;

/**
 * @author Anton Lamtev
 */
public final class GitException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4214808189050146491L;

    public GitException(final Throwable e) {
        super(e);
    }
}
