package org.accula.api.auth.oauth2.github;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
//TODO: move to github module?
public final class GithubUserInfoExtractor {
    private GithubUserInfoExtractor() {
    }

    public static GithubUserShortInfo extractUser(final Map<String, Object> rawUserInfo) {
        final var id = (Integer) rawUserInfo.get("id");
        final var login = (String) rawUserInfo.get("login");
        var name = (String) rawUserInfo.get("name");
        name = name != null ? name : "";
        final var avatar = (String) rawUserInfo.get("avatar_url");

        return new GithubUserShortInfo(id.longValue(), login, name, avatar);
    }
}
