package org.accula.api.auth.util;

import org.jetbrains.annotations.NotNull;
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

    public static void set(@NotNull final MultiValueMap<String, ResponseCookie> cookies,
                           @NotNull final String refreshToken,
                           @NotNull final Duration expiresIn) {
        cookies.set(REFRESH_TOKEN_COOKIE_NAME, ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .maxAge(expiresIn)
                .path("/")
                .httpOnly(true)
                .build());
    }

    @NotNull
    public static Optional<String> get(@NotNull final MultiValueMap<String, HttpCookie> cookies) {
        return Optional.ofNullable(cookies.getFirst(REFRESH_TOKEN_COOKIE_NAME))
                .map(HttpCookie::getValue);
    }
}
