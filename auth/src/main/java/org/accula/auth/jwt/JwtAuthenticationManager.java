package org.accula.auth.jwt;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public final class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    @Override
    public Mono<Authentication> authenticate(final Authentication authentication) {
        authentication.setAuthenticated(true);
        return Mono.just(authentication);
    }
}
