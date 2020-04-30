package org.accula.auth.oauth2;

import lombok.RequiredArgsConstructor;
import org.accula.auth.github.util.UserInfoExtractor;
import org.accula.auth.jwt.crypto.Jwt;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RequiredArgsConstructor
public final class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private static final String RESPONSE_BODY_FORMAT = "{\"jwt\": \"%s\", \"expirationDate\": \"%s\"}";
    private final Jwt jwt;

    @Override
    public Mono<Void> onAuthenticationSuccess(final WebFilterExchange exchange, final Authentication authentication) {
        return Mono
                .just(authentication)
                .filter(principal -> principal instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(token -> formResponse(exchange, token));
    }

    @NotNull
    private Mono<Void> formResponse(@NotNull final WebFilterExchange exchange,
                                    @NotNull final OAuth2AuthenticationToken token) {
        final var response = exchange.getExchange().getResponse();

        return response.writeWith(Mono.fromSupplier(() -> {
            final var userInfo = UserInfoExtractor.extractUser(token.getPrincipal().getAttributes());
            final var jwtDetails = jwt.generate(String.valueOf(userInfo.id));
            final var respBody = String.format(RESPONSE_BODY_FORMAT, jwtDetails.token, jwtDetails.expirationDate).getBytes(UTF_8);

            response.getHeaders().setContentType(APPLICATION_JSON);
            response.getHeaders().setContentLength(respBody.length);

            return response.bufferFactory().wrap(respBody);
        }));
    }
}
