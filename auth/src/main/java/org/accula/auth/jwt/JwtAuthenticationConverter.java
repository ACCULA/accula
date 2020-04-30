package org.accula.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.auth.github.User;
import org.accula.auth.jwt.crypto.Jwt;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

    @Override
    public Mono<Authentication> convert(final ServerWebExchange exchange) {
        final var authorizationHeader = Optional
                .ofNullable(exchange.getRequest().getHeaders().getFirst(AUTHORIZATION));

        return Mono
                .justOrEmpty(authorizationHeader)
                .filter(JwtAuthenticationConverter::hasBearerToken)
                .map(this::buildAuthentication)
                .onErrorResume(e -> Mono.empty());
    }

    private static boolean hasBearerToken(@NotNull final String header) {
        return header.startsWith(BEARER_PREFIX);
    }

    @NotNull
    private Authentication buildAuthentication(@NotNull final String header) {
        final var user = parseUser(header);
        return new JwtAuthentication(user);
    }

    @NotNull
    private User parseUser(@NotNull final String header) {
        final var tokenPart = header.substring(JWT_TOKEN_POSITION);
        final var userId = Long.parseLong(jwt.verify(tokenPart));

        return new User(userId);
    }
}
