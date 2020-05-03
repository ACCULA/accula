package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.RefreshTokenCookies;
import org.accula.api.db.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Web filter that refreshes an access token using refresh token provided in cookies.
 * <p>New refresh token replaces the previous one in DB
 * ({@link RefreshTokenRepository}) as well as in client cookies.
 * <p>Response is constructed using {@link JwtAccessTokenResponseProducer}.
 *
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class JwtRefreshFilter implements WebFilter {
    private final ServerWebExchangeMatcher endpointMatcher;
    private final JwtAccessTokenResponseProducer responseProducer;
    private final Jwt jwt;
    private final Duration refreshExpiresIn;
    private final RefreshTokenRepository refreshTokens;

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return endpointMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .flatMap(match -> doRefreshToken(exchange))
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()));
    }

    private Mono<Void> doRefreshToken(final ServerWebExchange exchange) {
        return Mono
                .justOrEmpty(RefreshTokenCookies.get(exchange.getRequest().getCookies()))
                .flatMap(refreshToken -> {
                    final var userIdString = jwt.verify(refreshToken);
                    final var userId = Long.valueOf(userIdString);
                    final var newRefreshTokenDetails = jwt.generate(userIdString, refreshExpiresIn);
                    final var newRefreshToken = newRefreshTokenDetails.getToken();
                    final var newRefreshTokenExpirationDate = newRefreshTokenDetails.getExpirationDate();

                    return refreshTokens
                            .replaceRefreshToken(userId, refreshToken, newRefreshToken, newRefreshTokenExpirationDate)
                            .then(responseProducer.formResponse(exchange, userId, newRefreshToken));
                })
                .onErrorResume(e -> Mono.fromRunnable(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }));
    }
}
