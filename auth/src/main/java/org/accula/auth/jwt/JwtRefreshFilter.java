package org.accula.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class JwtRefreshFilter implements WebFilter {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "accula_refresh_token";
    private final ServerWebExchangeMatcher pathMatcher;

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return pathMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .then(JwtRefreshFilter.extractRefreshToken(exchange))
                .then()
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()));
    }

    @NotNull
    private static Mono<String> extractRefreshToken(@NotNull final ServerWebExchange exchange) {
        final var refreshToken = Optional
                .ofNullable(exchange.getRequest().getCookies().getFirst(REFRESH_TOKEN_COOKIE_NAME))
                .map(HttpCookie::getValue);

        return Mono.justOrEmpty(refreshToken);
    }
}
