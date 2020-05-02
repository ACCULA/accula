package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.RefreshTokenCookies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Produces a response with the access token and its expiration date as a part of
 * body JSON (see {@code RESPONSE_BODY_FORMAT}), and refresh token included in cookies.
 *
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class JwtAccessTokenResponseProducer {
    private static final String RESPONSE_BODY_FORMAT = "{\"access_token\":\"%s\",\"expirationDate\":\"%s\"}";

    private final Jwt jwt;
    private final Duration accessExpiresIn;
    private final Duration refreshExpiresIn;

    public Mono<Void> formResponse(final ServerWebExchange exchange, final Long userId, final String refreshToken) {
        final var response = exchange.getResponse();

        return response.writeWith(Mono.fromSupplier(() -> {
            final var accessTokenDetails = jwt.generate(userId.toString(), accessExpiresIn);
            final var respBody = String.format(
                    RESPONSE_BODY_FORMAT,
                    accessTokenDetails.getToken(),
                    accessTokenDetails.getExpirationDate().toString()
            ).getBytes(UTF_8);

            response.getHeaders().setContentType(APPLICATION_JSON);
            response.getHeaders().setContentLength(respBody.length);

            RefreshTokenCookies.set(response.getCookies(), refreshToken, refreshExpiresIn);

            return response.bufferFactory().wrap(respBody);
        }));
    }
}