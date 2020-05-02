package org.accula.api.auth.oauth2.github;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
public final class UserInfoExtractor {
    private UserInfoExtractor() {
    }

    @NotNull
    public static UserShortInfo extractUser(@NotNull final Map<String, Object> rawUserInfo) {
        final var id = (Integer) rawUserInfo.get("id");
        final var login = (String) rawUserInfo.get("login");
        final var name = (String) rawUserInfo.get("name");

        return new UserShortInfo(id.longValue(), login, name);
    }
}
