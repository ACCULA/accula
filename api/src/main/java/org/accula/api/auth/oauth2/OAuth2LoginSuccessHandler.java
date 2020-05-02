package org.accula.api.auth.oauth2;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.oauth2.github.UserInfoExtractor;
import org.accula.api.auth.util.RefreshTokenCookies;
import org.accula.api.db.RefreshTokenRepository;
import org.accula.api.db.UserRepository;
import org.accula.api.db.dto.RefreshToken;
import org.accula.api.db.dto.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class completes login with Github OAuth2 by signing-in or signing-up to our service.
 * It forms a response with our own access token, its expiration date, and refresh token:
 * <p>
 * 1. If DB ({@link UserRepository}) contains a user with obtained Github id and differ Github access token,
 * then Github access token is updated.
 * <p>
 * 2. If DB ({@link UserRepository}) doesn't contain a user with obtained Github,
 * then new user with provided Github id is created.
 * <p>
 * 3. We generate our own access token (JWT with user id sub and short lifetime)
 * which is then included in response body json ({@link RESPONSE_BODY_FORMAT}). We suppose client will store it in memory.
 * <p>
 * 4. We generate our own refresh token (also JWT with user id sub but longer lifetime)
 * which is then saved in DB ({@link RefreshTokenRepository}) and included in response http-only cookie.
 *
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private static final String RESPONSE_BODY_FORMAT = "{\"jwt\":\"%s\",\"expirationDate\":\"%s\"}";
    private final Jwt jwt;
    private final Duration accessExpiresIn;
    private final Duration refreshExpiresIn;
    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    @Override
    public Mono<Void> onAuthenticationSuccess(final WebFilterExchange exchange, final Authentication authentication) {
        return Mono.defer(() -> {
            if (!(authentication instanceof OAuth2AuthenticationToken)) {
                return Mono.empty();
            }

            final var authenticationToken = (OAuth2AuthenticationToken) authentication;
            final var userGithubInfo = UserInfoExtractor.extractUser(authenticationToken.getPrincipal().getAttributes());

            return authorizedClientService
                    .loadAuthorizedClient(authenticationToken.getAuthorizedClientRegistrationId(), authenticationToken.getName())
                    .map(OAuth2AuthorizedClient::getAccessToken)
                    .flatMap(accessToken -> users
                            .findByGithubId(userGithubInfo.getId())
                            .filter(user -> !user.getGithubAccessToken().equals(accessToken.getTokenValue()))
                            .flatMap(user -> users
                                    .setNewAccessToken(user.getGithubId(), accessToken.getTokenValue())
                                    .thenReturn(User.of(Objects.requireNonNull(user.getId()), userGithubInfo.getId(), accessToken.getTokenValue())))
                            .switchIfEmpty(users.save(User.of(userGithubInfo.getId(), accessToken.getTokenValue()))))
                    .flatMap(user -> {
                        final var userId = Objects.requireNonNull(user.getId());
                        final var refreshJwtDetails = jwt.generate(user.getId().toString(), refreshExpiresIn);

                        return refreshTokens
                                .save(RefreshToken.of(userId, refreshJwtDetails.getToken(), refreshJwtDetails.getExpirationDate()))
                                .thenReturn(userId)
                                .zipWith(Mono.just(refreshJwtDetails));
                    })
                    .flatMap(userIdAndRefreshToken -> {
                        final var userId = userIdAndRefreshToken.getT1();
                        final var refreshToken = userIdAndRefreshToken.getT2();

                        return formResponse(exchange, userId, refreshToken.getToken());
                    });
        });
    }

    private Mono<Void> formResponse(final WebFilterExchange exchange,
                                    final Long userId,
                                    final String refreshToken) {
        final var response = exchange.getExchange().getResponse();

        return response.writeWith(Mono.fromSupplier(() -> {
            final var jwtDetails = jwt.generate(userId.toString(), accessExpiresIn);
            final var respBody = String.format(
                    RESPONSE_BODY_FORMAT,
                    jwtDetails.getToken(),
                    jwtDetails.getExpirationDate().toString()
            ).getBytes(UTF_8);

            response.getHeaders().setContentType(APPLICATION_JSON);
            response.getHeaders().setContentLength(respBody.length);

            RefreshTokenCookies.set(response.getCookies(), refreshToken, refreshExpiresIn);

            return response.bufferFactory().wrap(respBody);
        }));
    }
}
