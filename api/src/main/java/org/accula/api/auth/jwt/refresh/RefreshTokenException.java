package org.accula.api.auth.jwt.refresh;

import lombok.Getter;

@Getter
final class RefreshTokenException extends RuntimeException {
    private final String reason;

    RefreshTokenException(final Reason reason) {
        this.reason = reason.name();
    }

    enum Reason {
        MISSING_TOKEN,
        TOKEN_VERIFICATION_FAILED,
        UNABLE_TO_REPLACE_IN_DB,
    }

    static boolean isInstanceOf(final Throwable t) {
        return t instanceof RefreshTokenException;
    }
}
