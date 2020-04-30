package org.accula.auth.jwt;

import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

/**
 * @author Anton Lamtev
 */
public final class JwtAuthFilter extends AuthenticationWebFilter {
    public JwtAuthFilter() {
        super(new JwtAuthenticationManager());
    }
}
