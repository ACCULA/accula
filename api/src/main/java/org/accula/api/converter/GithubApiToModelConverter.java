package org.accula.api.converter;

import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiSnapshot;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.util.Checks;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
@Component
@SuppressWarnings("PMD.ConfusingTernary")
public final class GithubApiToModelConverter {
    private GithubApiToModelConverter() {
    }

    public static GithubRepo convert(final GithubApiRepo apiRepo) {
        return new GithubRepo(
                apiRepo.id(),
                apiRepo.name(),
                apiRepo.isPrivate(),
                orEmpty(apiRepo.description()),
                convert(apiRepo.owner())
        );
    }

    public static GithubUser convert(final GithubApiUser apiUser) {
        return new GithubUser(
                apiUser.id(),
                apiUser.login(),
                apiUser.name(),
                apiUser.avatarUrl(),
                apiUser.type() == GithubApiUser.Type.ORGANIZATION
        );
    }

    public static GithubUser convert(final Map<String, Object> attributes) {
        final var id = ((Number) attributes.get("id")).longValue();
        final var login = (String) attributes.get("login");
        final var name = (String) attributes.get("name");
        final var avatar = (String) attributes.get("avatar_url");
        return new GithubUser(id, login, name, avatar, false);
    }

    public static Snapshot.PullInfo convertInfo(final GithubApiPull pull) {
        return Snapshot.PullInfo.of(pull.id(), pull.number());
    }

    public static Snapshot convert(final GithubApiSnapshot snapshot, final GithubApiPull pull) {
        return Snapshot.builder()
                .commit(Commit.shaOnly(snapshot.sha()))
                .branch(snapshot.ref())
                .pullInfo(convertInfo(pull))
                .repo(convert(Checks.notNull(snapshot.repo(), "GithubApiPull repo")))
                .build();
    }

    public static Pull convert(final GithubApiPull pull) {
        return Pull.builder()
                .id(pull.id())
                .number(pull.number())
                .title(pull.title())
                .isOpen(pull.state() == GithubApiPull.State.OPEN)
                .createdAt(pull.createdAt())
                .updatedAt(pull.updatedAt())
                .head(convert(pull.head(), pull))
                .base(convert(pull.base(), pull))
                .author(convert(pull.user()))
                .build();
    }

    private static String orEmpty(@Nullable final String s) {
        return s != null && !s.isBlank() ? s : "";
    }
}
