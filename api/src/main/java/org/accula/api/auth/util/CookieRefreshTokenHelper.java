package org.accula.api.auth.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class CookieRefreshTokenHelper {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final String refreshTokenEndpointPath;

    public void set(final MultiValueMap<String, ResponseCookie> cookies, final String refreshToken, final Duration expiresIn) {
        cookies.set(REFRESH_TOKEN_COOKIE_NAME, ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .maxAge(expiresIn)
                .path(refreshTokenEndpointPath)
                .httpOnly(true)
                .build());
    }

    public Optional<String> get(final MultiValueMap<String, HttpCookie> cookies) {
        return Optional.ofNullable(cookies.getFirst(REFRESH_TOKEN_COOKIE_NAME))
                .map(HttpCookie::getValue);
    }
}
