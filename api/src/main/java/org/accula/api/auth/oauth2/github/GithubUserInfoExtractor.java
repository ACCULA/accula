package org.accula.api.auth.oauth2.github;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
public final class GithubUserInfoExtractor {
    private GithubUserInfoExtractor() {
    }

    public static GithubUserShortInfo extractUser(final Map<String, Object> rawUserInfo) {
        final var id = (Integer) rawUserInfo.get("id");
        final var login = (String) rawUserInfo.get("login");
        final var name = (String) rawUserInfo.get("name");
        final var avatar = (String) rawUserInfo.get("avatar_url");

        return new GithubUserShortInfo(id.longValue(), login, name, avatar);
    }
}
