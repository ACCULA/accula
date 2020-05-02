package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.github.User;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.RefreshTokenCookies;
import org.accula.api.db.RefreshTokenRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int JWT_TOKEN_POSITION = BEARER_PREFIX.length();

    private final Jwt jwt;
    private final Duration refreshExpiresIn;
    private final RefreshTokenRepository refreshTokens;

    private static boolean hasBearerToken(@NotNull final String header) {
        return header.startsWith(BEARER_PREFIX);
    }

    @Override
    public Mono<Authentication> convert(final ServerWebExchange exchange) {
        final var request = exchange.getRequest();
        final var authorizationHeader = Optional
                .ofNullable(request.getHeaders().getFirst(AUTHORIZATION));

        return Mono
                .justOrEmpty(authorizationHeader)
                .filter(JwtAuthenticationConverter::hasBearerToken)
                .map(this::tryAuthenticateWithBearerToken)
                .onErrorResume(e -> tryAuthenticateWithRefreshToken(RefreshTokenCookies.get(request.getCookies()), exchange.getResponse()));
    }

    @NotNull
    private Authentication tryAuthenticateWithBearerToken(@NotNull final String header) {
        final var bearerToken = header.substring(JWT_TOKEN_POSITION);
        return new JwtAuthentication(new User(decodeUserId(bearerToken)));
    }

    @NotNull
    private Long decodeUserId(@NotNull final String jwtToken) {
        return Long.valueOf(jwt.verify(jwtToken));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @NotNull
    private Mono<Authentication> tryAuthenticateWithRefreshToken(@NotNull final Optional<String> refreshToken,
                                                                 @NotNull final ServerHttpResponse response) {
        return Mono
                .justOrEmpty(refreshToken)
                .flatMap(token -> {
                    final var userId = decodeUserId(token);
                    final var newToken = jwt.generate(userId.toString(), refreshExpiresIn);
                    final var newTokenJwt = newToken.getToken();
                    final var newTokenExpirationDate = newToken.getExpirationDate();
                    return refreshTokens
                            .replaceRefreshToken(userId, token, newTokenJwt, newTokenExpirationDate)
                            .then(Mono.fromRunnable(() -> RefreshTokenCookies.set(response.getCookies(), newTokenJwt, refreshExpiresIn)))
                            .thenReturn((Authentication) new JwtAuthentication(new User(userId)));
                })
                .onErrorResume(e -> Mono.empty());
    }
}
