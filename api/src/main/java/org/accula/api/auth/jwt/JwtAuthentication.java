package org.accula.api.auth.jwt;

import org.accula.api.auth.github.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

/**
 * @author Anton Lamtev
 */
public final class JwtAuthentication extends AbstractAuthenticationToken implements Authentication {
    private final User user;

    public JwtAuthentication(@NotNull final User user) {
        super(Collections.emptyList());
        this.user = user;
    }

    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    @Override
    @NotNull
    public User getPrincipal() {
        return user;
    }
}
