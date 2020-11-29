package org.accula.api.auth.jwt.refresh;

import lombok.Getter;

import java.io.Serial;

/**
 * @author Anton Lamtev
 */
@Getter
final class RefreshTokenException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1081904770207522419L;
    private final String reason;

    RefreshTokenException(final Reason reason) {
        this.reason = reason.name();
    }

    enum Reason {
        MISSING_TOKEN,
        TOKEN_VERIFICATION_FAILED,
        UNABLE_TO_REPLACE_IN_DB,
    }
}
