package org.accula.api.auth.jwt.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.auth.jwt.JwtAccessTokenResponseProducer;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.CookieRefreshTokenHelper;
import org.accula.api.db.repo.RefreshTokenRepo;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static java.util.function.Predicate.not;
import static org.accula.api.auth.jwt.refresh.RefreshTokenException.Reason.MISSING_TOKEN;
import static org.accula.api.auth.jwt.refresh.RefreshTokenException.Reason.TOKEN_VERIFICATION_FAILED;
import static org.accula.api.auth.jwt.refresh.RefreshTokenException.Reason.UNABLE_TO_REPLACE_IN_DB;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;

/**
 * Web filter that refreshes an access token using refresh token provided in cookies.
 * <p>New refresh token replaces the previous one in DB
 * ({@link RefreshTokenRepo}) as well as in client cookies.
 * <p>Response is constructed using {@link JwtAccessTokenResponseProducer}.
 *
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Slf4j
@RequiredArgsConstructor
public final class JwtRefreshFilter implements WebFilter {
    private final static RefreshTokenException MISSING_TOKEN_EXCEPTION = new RefreshTokenException(MISSING_TOKEN);
    private final static RefreshTokenException TOKEN_VERIFICATION_EXCEPTION = new RefreshTokenException(TOKEN_VERIFICATION_FAILED);
    private final static RefreshTokenException UNABLE_REPLACE_IN_DB_EXCEPTION = new RefreshTokenException(UNABLE_TO_REPLACE_IN_DB);

    private final ServerWebExchangeMatcher endpointMatcher;
    private final CookieRefreshTokenHelper cookieRefreshTokenHelper;
    private final JwtAccessTokenResponseProducer responseProducer;
    private final Jwt jwt;
    private final Duration refreshExpiresIn;
    private final RefreshTokenRepo refreshTokens;
    private final String webUrl;

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return endpointMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
                .flatMap(match -> {
                    exchange.getResponse().getHeaders().setAccessControlAllowOrigin(webUrl);
                    exchange.getResponse().getHeaders().setAccessControlAllowHeaders(List.of(ACCESS_CONTROL_ALLOW_ORIGIN));
                    exchange.getResponse().getHeaders().setAccessControlAllowCredentials(true);
                    return doRefreshToken(exchange);
                });
    }

    private Mono<Void> doRefreshToken(final ServerWebExchange exchange) {
        return Mono
                .justOrEmpty(cookieRefreshTokenHelper.get(exchange.getRequest().getCookies()))
                .switchIfEmpty(Mono.error(MISSING_TOKEN_EXCEPTION))
                .flatMap(refreshToken -> {
                    final var userIdString = jwt.verify(refreshToken);
                    final var userId = Long.valueOf(userIdString);
                    final var newRefreshTokenDetails = jwt.generate(userIdString, refreshExpiresIn);
                    final var newRefreshToken = newRefreshTokenDetails.getToken();
                    final var newRefreshTokenExpirationDate = newRefreshTokenDetails.getExpirationDate();

                    return refreshTokens
                            .replaceRefreshToken(userId, refreshToken, newRefreshToken, newRefreshTokenExpirationDate)
                            .onErrorMap(e -> UNABLE_REPLACE_IN_DB_EXCEPTION)
                            .then(responseProducer.formSuccessBody(exchange, userId, newRefreshToken));
                })
                .onErrorMap(not(RefreshTokenException.class::isInstance), e -> TOKEN_VERIFICATION_EXCEPTION)
                .onErrorResume(RefreshTokenException.class, e -> handleRefreshTokenFailure(e, exchange));
    }

    private Mono<Void> handleRefreshTokenFailure(final RefreshTokenException failure, final ServerWebExchange exchange) {
        log.info("Failed to refresh token with reason: {}", failure.getReason());
        return responseProducer.formFailureBody(exchange);
    }
}
