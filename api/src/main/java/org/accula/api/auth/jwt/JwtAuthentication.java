package org.accula.api.auth.jwt;

import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.Serial;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public final class JwtAuthentication extends AbstractAuthenticationToken implements Authentication {
    @Serial
    private static final long serialVersionUID = -7833231338530910449L;

    private final AuthorizedUser user;

    public JwtAuthentication(final AuthorizedUser user) {
        super(List.of());
        this.user = user;
    }

    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthorizedUser getPrincipal() {
        return user;
    }
}
