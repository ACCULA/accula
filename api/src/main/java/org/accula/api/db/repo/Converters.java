package org.accula.api.db.repo;

import io.r2dbc.spi.Row;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.User;

import java.util.Objects;

/**
 * @author Anton Lamtev
 */
final class Converters {
    private Converters() {
    }

    static GithubUser convertUser(final Row row,
                                  final String id,
                                  final String login,
                                  final String name,
                                  final String avatar,
                                  final String organization) {
        return new GithubUser(
                value(row, id, Long.class),
                value(row, login, String.class),
                value(row, name, String.class),
                value(row, avatar, String.class),
                value(row, organization, Boolean.class)
        );
    }

    static GithubRepo convertRepo(final Row row,
                                  final String id,
                                  final String name,
                                  final String description,
                                  final String ownerId,
                                  final String ownerLogin,
                                  final String ownerName,
                                  final String ownerAvatar,
                                  final String ownerOrganization) {
        return new GithubRepo(
                value(row, id, Long.class),
                row.get(name, String.class),
                value(row, description, String.class),
                convertUser(row, ownerId, ownerLogin, ownerName, ownerAvatar, ownerOrganization)
        );
    }

    static Pull.Marker convertPullMarker(final Row row,
                                         final String sha,
                                         final String branch,
                                         final String repoId,
                                         final String repoName,
                                         final String repoDescription,
                                         final String repoOwnerId,
                                         final String repoOwnerLogin,
                                         final String repoOwnerName,
                                         final String repoOwnerAvatar,
                                         final String repoOwnerOrganization) {
        return new Pull.Marker(
                convertCommit(row, sha),
                value(row, branch, String.class),
                convertRepo(row,
                        repoId,
                        repoName,
                        repoDescription,
                        repoOwnerId,
                        repoOwnerLogin,
                        repoOwnerName,
                        repoOwnerAvatar,
                        repoOwnerOrganization)
        );
    }

    static Commit convertCommit(final Row row, final String sha) {
        return new Commit(value(row, sha, String.class));
    }

    static User convertUser(final Row row,
                            final String id,
                            final String accessToken,
                            final String githubId,
                            final String githubLogin,
                            final String githubName,
                            final String githubAvatar,
                            final String githubOrganization) {
        return new User(
                value(row, id, Long.class),
                value(row, accessToken, String.class),
                convertUser(row, githubId, githubLogin, githubName, githubAvatar, githubOrganization)
        );
    }

    static <T> T value(final Row row, final String name, final Class<T> clazz) {
        return Objects.requireNonNull(row.get(name, clazz));
    }
}
