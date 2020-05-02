package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Performs user authentication and authorization using JWT Bearer token provided
 *
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int JWT_TOKEN_POSITION = BEARER_PREFIX.length();

    private final Jwt jwt;

    private static boolean hasBearerToken(final String header) {
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
                .onErrorResume(e -> Mono.empty());
    }

    private Authentication tryAuthenticateWithBearerToken(final String header) {
        final var bearerToken = header.substring(JWT_TOKEN_POSITION);
        return new JwtAuthentication(new AuthorizedUser(decodeUserId(bearerToken)));
    }

    private Long decodeUserId(final String jwtToken) {
        return Long.valueOf(jwt.verify(jwtToken));
    }
}
