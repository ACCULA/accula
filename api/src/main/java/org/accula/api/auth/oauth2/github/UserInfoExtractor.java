package org.accula.api.auth.oauth2.github;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
public final class UserInfoExtractor {
    private UserInfoExtractor() {
    }

    public static UserShortInfo extractUser(final Map<String, Object> rawUserInfo) {
        final var id = (Integer) rawUserInfo.get("id");
        final var login = (String) rawUserInfo.get("login");
        final var name = (String) rawUserInfo.get("name");

        return new UserShortInfo(id.longValue(), login, name);
    }
}
