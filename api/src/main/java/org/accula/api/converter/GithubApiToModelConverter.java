package org.accula.api.converter;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUser;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
@Component
public final class GithubApiToModelConverter {
    private static final String EMPTY = "";

    public GithubRepo convert(final GithubApiRepo apiRepo) {
        return new GithubRepo(
                apiRepo.getId(),
                apiRepo.getName(),
                orEmpty(apiRepo.getDescription()),
                convert(apiRepo.getOwner())
        );
    }

    public GithubUser convert(final GithubApiUser apiUser) {
        return new GithubUser(
                apiUser.getId(),
                apiUser.getLogin(),
                orEmpty(apiUser.getName()),
                apiUser.getAvatarUrl(),
                apiUser.getType() == GithubApiUser.Type.ORGANIZATION
        );
    }

    public GithubUser convert(final Map<String, Object> attributes) {
        final var id = ((Number) attributes.get("id")).longValue();
        final var login = (String) attributes.get("login");
        final var name = (String) attributes.getOrDefault("name", "");
        final var avatar = (String) attributes.get("avatar_url");
        return new GithubUser(id, login, name, avatar, false);
    }

    private static String orEmpty(@Nullable final String s) {
        return s != null ? s : EMPTY;
    }
}
