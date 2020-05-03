package org.accula.api.auth.util;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Anton Lamtev
 */
public final class RefreshTokenCookies {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private RefreshTokenCookies() {
    }

    public static void set(final MultiValueMap<String, ResponseCookie> cookies,
                           final String refreshToken,
                           final Duration expiresIn) {
        cookies.set(REFRESH_TOKEN_COOKIE_NAME, ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .maxAge(expiresIn)
                .path("/")
                .httpOnly(true)
                .build());
    }

    public static Optional<String> get(final MultiValueMap<String, HttpCookie> cookies) {
        return Optional.ofNullable(cookies.getFirst(REFRESH_TOKEN_COOKIE_NAME))
                .map(HttpCookie::getValue);
    }
}
