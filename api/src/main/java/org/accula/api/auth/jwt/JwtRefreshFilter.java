package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.CookieRefreshTokenHelper;
import org.accula.api.db.RefreshTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static java.util.function.Predicate.not;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Web filter that refreshes an access token using refresh token provided in cookies.
 * <p>New refresh token replaces the previous one in DB
 * ({@link RefreshTokenRepository}) as well as in client cookies.
 * <p>Response is constructed using {@link JwtAccessTokenResponseProducer}.
 *
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@RequiredArgsConstructor
public final class JwtRefreshFilter implements WebFilter {
    private final static ResponseStatusException BAD_REQUEST_EXCEPTION = new ResponseStatusException(BAD_REQUEST);

    private final ServerWebExchangeMatcher endpointMatcher;
    private final CookieRefreshTokenHelper cookieRefreshTokenHelper;
    private final JwtAccessTokenResponseProducer responseProducer;
    private final Jwt jwt;
    private final Duration refreshExpiresIn;
    private final RefreshTokenRepository refreshTokens;

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return endpointMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
                .flatMap(match -> {
                    exchange.getResponse().getHeaders().setAccessControlAllowOrigin("http://localhost:3000");
                    exchange.getResponse().getHeaders().setAccessControlAllowHeaders(List.of("Access-Control-Allow-Origin"));
                    exchange.getResponse().getHeaders().setAccessControlAllowCredentials(true);
                    return doRefreshToken(exchange);
                });
    }

    private Mono<Void> doRefreshToken(final ServerWebExchange exchange) {
        return Mono
                .justOrEmpty(cookieRefreshTokenHelper.get(exchange.getRequest().getCookies()))
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
                .onErrorMap(not(BAD_REQUEST_EXCEPTION::equals), e -> BAD_REQUEST_EXCEPTION);
    }
}
