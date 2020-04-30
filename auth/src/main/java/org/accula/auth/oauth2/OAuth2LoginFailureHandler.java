package org.accula.auth.oauth2;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public final class OAuth2LoginFailureHandler implements ServerAuthenticationFailureHandler {
    @Override
    public Mono<Void> onAuthenticationFailure(final WebFilterExchange webFilterExchange,
                                              final AuthenticationException exception) {
        return Mono.error(exception);
    }
}
