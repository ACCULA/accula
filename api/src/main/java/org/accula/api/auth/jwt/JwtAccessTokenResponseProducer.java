package org.accula.api.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.util.CookieRefreshTokenHelper;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Produces a response in the form of HTTP-redirect or body in JSON format.
 *
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@RequiredArgsConstructor
public final class JwtAccessTokenResponseProducer {
    private static final String RESPONSE_BODY_FORMAT = "{\"accessToken\":\"%s\"}";
    private static final byte[] FAILURE_BODY = String.format(RESPONSE_BODY_FORMAT, "").getBytes(UTF_8);

    private final Jwt jwt;
    private final Duration accessExpiresIn;
    private final Duration refreshExpiresIn;
    private final CookieRefreshTokenHelper cookieRefreshTokenHelper;
    private final String webUrl;

    /**
     * Produces a response with redirect (i.e. HttpStatus == 302) to the static web-server using the following pattern:
     * {@code ${webUrl}/oauth2/redirect?accessToken=${accessToken}}.
     */
    public Mono<Void> formSuccessRedirect(final ServerWebExchange exchange, final Long userId, final String refreshToken) {
        return Mono.fromRunnable(() -> {
            final var response = exchange.getResponse();
            final var accessTokenDetails = jwt.generate(userId.toString(), accessExpiresIn);
            final var location = URI.create(webUrl + "/oauth2/redirect?accessToken=" + accessTokenDetails.token());
            response.getHeaders().setLocation(location);
            response.setStatusCode(FOUND);
            cookieRefreshTokenHelper.set(response.getCookies(), refreshToken, refreshExpiresIn);
        });
    }

    /**
     * Produces a response with the access token as a part of body JSON
     * (see {@code RESPONSE_BODY_FORMAT}), and refresh token included in cookies
     */
    public Mono<Void> formSuccessBody(final ServerWebExchange exchange, final Long userId, final String refreshToken) {
        final var response = exchange.getResponse();

        return response.writeWith(Mono.fromSupplier(() -> {
            final var accessTokenDetails = jwt.generate(userId.toString(), accessExpiresIn);
            final var respBody = String.format(RESPONSE_BODY_FORMAT, accessTokenDetails.token()).getBytes(UTF_8);

            response.setStatusCode(OK);
            response.getHeaders().setContentType(APPLICATION_JSON);
            response.getHeaders().setContentLength(respBody.length);
            cookieRefreshTokenHelper.set(response.getCookies(), refreshToken, refreshExpiresIn);

            return response.bufferFactory().wrap(respBody);
        }));
    }

    /**
     * Produces a response with an empty access token
     */
    public Mono<Void> formFailureBody(final ServerWebExchange exchange) {
        final var response = exchange.getResponse();

        return response.writeWith(Mono.fromSupplier(() -> {
            response.setStatusCode(OK);
            response.getHeaders().setContentType(APPLICATION_JSON);
            response.getHeaders().setContentLength(FAILURE_BODY.length);

            return response.bufferFactory().wrap(FAILURE_BODY);
        }));
    }
}
