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
        return GithubRepo.builder()
            .id(apiRepo.id())
            .name(apiRepo.name())
            .isPrivate(apiRepo.isPrivate())
            .description(orEmpty(apiRepo.description()))
            .owner(convert(apiRepo.owner()))
            .build();
    }

    public static GithubUser convert(final GithubApiUser apiUser) {
        return GithubUser.builder()
            .id(apiUser.id())
            .login(apiUser.login())
            .name(apiUser.name())
            .avatar(apiUser.avatarUrl())
            .isOrganization(apiUser.type() == GithubApiUser.Type.ORGANIZATION)
            .build();
    }

    public static GithubUser convert(final Map<String, Object> attributes) {
        final var id = ((Number) attributes.get("id")).longValue();
        final var login = (String) attributes.get("login");
        final var name = (String) attributes.get("name");
        final var avatar = (String) attributes.get("avatar_url");
        return GithubUser.builder()
            .id(id)
            .login(login)
            .name(name)
            .avatar(avatar)
            .isOrganization(false)
            .build();
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
