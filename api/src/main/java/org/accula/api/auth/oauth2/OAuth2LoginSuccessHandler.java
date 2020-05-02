package org.accula.api.auth.oauth2;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.oauth2.github.UserInfoExtractor;
import org.accula.api.auth.util.RefreshTokenCookies;
import org.accula.api.db.RefreshTokenRepository;
import org.accula.api.db.UserRepository;
import org.accula.api.db.dto.RefreshToken;
import org.accula.api.db.dto.User;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    private Mono<Void> formResponse(@NotNull final WebFilterExchange exchange,
                                    @NotNull final Long userId,
                                    @NotNull final String refreshToken) {
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
