package org.accula.auth.github.util;

import org.accula.auth.github.GhUserShortInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class UserInfoExtractor {
    private UserInfoExtractor() {
    }

    @NotNull
    public static GhUserShortInfo extractUser(@NotNull final Map<String, Object> rawUserInfo) {
        final var id = Long.valueOf((Integer) rawUserInfo.get("id"));
        final var login = (String) rawUserInfo.get("login");
        final var name = (String) rawUserInfo.get("name");

        return new GhUserShortInfo(id, login, name);
    }
}
