package org.accula.api.auth;

import org.accula.api.auth.jwt.AuthorizedUser;
import org.accula.api.auth.jwt.JwtAuthentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

/**
 * Provides the current authorized user stored in a {@link org.springframework.security.core.context.SecurityContext}
 *
 * @author Anton Lamtev
 */
public final class CurrentAuthorizedUserProvider {
    private CurrentAuthorizedUserProvider() {
    }

    public static Mono<AuthorizedUser> get() {
        return ReactiveSecurityContextHolder
                .getContext()
                .flatMap(ctx -> {
                    final var authentication = ctx.getAuthentication();
                    if (!(authentication instanceof JwtAuthentication)) {
                        return Mono.empty();
                    }

                    return Mono.just(((JwtAuthentication) authentication).getPrincipal());
                });
    }
}
