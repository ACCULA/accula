package org.accula.api.auth.oauth2;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.JwtAccessTokenResponseProducer;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.oauth2.github.GithubUserInfoExtractor;
import org.accula.api.db.RefreshTokenRepository;
import org.accula.api.db.UserRepository;
import org.accula.api.db.dto.RefreshToken;
import org.accula.api.db.dto.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * This class completes login with Github OAuth2 by signing-in or signing-up to our service.
 * It forms a response with our own access token, its expiration date, and refresh token:
 * <p>1. If DB ({@link UserRepository}) contains a user with obtained Github id and differ Github access token,
 * then Github access token is updated.
 * <p>2. If DB ({@link UserRepository}) doesn't contain a user with obtained Github id,
 * then new user with provided Github id is created.
 * <p>3. We generate our own access token (JWT with user id sub and short lifetime) which is then included
 * in response Location header URI using {@link JwtAccessTokenResponseProducer#formSuccessRedirect}.
 * We suppose client will store it in memory.
 * <p>4. We generate our own refresh token (also JWT with user id sub but longer lifetime)
 * which is then saved in DB ({@link RefreshTokenRepository}) and included in response http-only cookie.
 *
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@RequiredArgsConstructor
public final class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final JwtAccessTokenResponseProducer responseProducer;
    private final Jwt jwt;
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
            final var githubUser = GithubUserInfoExtractor.extractUser(authenticationToken.getPrincipal().getAttributes());
            final var githubId = githubUser.getId();
            final var githubLogin = githubUser.getLogin();
            final var githubName = githubUser.getName();

            return authorizedClientService
                    .loadAuthorizedClient(authenticationToken.getAuthorizedClientRegistrationId(), authenticationToken.getName())
                    .map(authorizedClient -> authorizedClient.getAccessToken().getTokenValue())
                    .flatMap(githubAccessToken -> users
                            .findByGithubId(githubId)
                            .filter(user -> user.shouldUpdateLoginOrAccessToken(githubLogin, githubAccessToken))
                            .flatMap(user -> users
                                    .updateGithubLoginAndAccessToken(githubId, githubLogin, githubAccessToken)
                                    .thenReturn(requireNonNull(user.getId())))
                            .switchIfEmpty(users
                                    .save(User.of(githubName, githubId, githubLogin, githubAccessToken))
                                    .map(user -> requireNonNull(user.getId()))))
                    .flatMap(userId -> {
                        final var refreshJwtDetails = jwt.generate(userId.toString(), refreshExpiresIn);
                        final var refreshToken = refreshJwtDetails.getToken();

                        return refreshTokens
                                .save(RefreshToken.of(userId, refreshToken, refreshJwtDetails.getExpirationDate()))
                                .thenReturn(userId)
                                .zipWith(Mono.just(refreshToken));
                    })
                    .flatMap(userIdAndRefreshToken -> {
                        final var userId = userIdAndRefreshToken.getT1();
                        final var refreshToken = userIdAndRefreshToken.getT2();

                        return responseProducer.formSuccessRedirect(exchange.getExchange(), userId, refreshToken);
                    });
        });
    }
}
