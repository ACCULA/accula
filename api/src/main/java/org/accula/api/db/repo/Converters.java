package org.accula.api.db.repo;

import io.r2dbc.spi.Row;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Anton Lamtev
 */
@SuppressWarnings({"PMD.ConfusingTernary", "PMD.UseObjectForClearerAPI", "PMD.ExcessiveParameterList"})
final class Converters {
    public static final String NOTHING = "";

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
                row.get(name, String.class),
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
                value(row, name, String.class),
                value(row, description, String.class),
                convertUser(row, ownerId, ownerLogin, ownerName, ownerAvatar, ownerOrganization)
        );
    }

    static CommitSnapshot convertCommitSnapshot(final Row row,
                                                final String sha,
                                                final String branch,
                                                final String pullId,
                                                final String repoId,
                                                final String repoName,
                                                final String repoDescription,
                                                final String repoOwnerId,
                                                final String repoOwnerLogin,
                                                final String repoOwnerName,
                                                final String repoOwnerAvatar,
                                                final String repoOwnerOrganization) {
        return CommitSnapshot.builder()
                .commit(convertCommit(row, sha))
                .branch(value(row, branch, String.class))
                .pullId(nullable(row, pullId, Long.class))
                .repo(convertRepo(row,
                        repoId,
                        repoName,
                        repoDescription,
                        repoOwnerId,
                        repoOwnerLogin,
                        repoOwnerName,
                        repoOwnerAvatar,
                        repoOwnerOrganization))
                .build();
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

    static Clone convertClone(final Row row,
                              final String id,
                              final String targetSha,
                              final String targetBranch,
                              final String targetPullId,
                              final String targetRepoId,
                              final String targetRepoName,
                              final String targetRepoDescription,
                              final String targetRepoOwnerId,
                              final String targetRepoOwnerLogin,
                              final String targetRepoOwnerName,
                              final String targetRepoOwnerAvatar,
                              final String targetRepoOwnerIsOrg,
                              final String targetFile,
                              final String targetFromLine,
                              final String targetToLine,
                              final String sourceSha,
                              final String sourceBranch,
                              final String sourcePullId,
                              final String sourceRepoId,
                              final String sourceRepoName,
                              final String sourceRepoDescription,
                              final String sourceRepoOwnerId,
                              final String sourceRepoOwnerLogin,
                              final String sourceRepoOwnerName,
                              final String sourceRepoOwnerAvatar,
                              final String sourceRepoOwnerIsOrg,
                              final String sourceFile,
                              final String sourceFromLine,
                              final String sourceToLine) {
        return Clone.builder()
                .id(value(row, id, Long.class))
                .targetSnapshot(convertCommitSnapshot(row,
                        targetSha,
                        targetBranch,
                        targetPullId,
                        targetRepoId,
                        targetRepoName,
                        targetRepoDescription,
                        targetRepoOwnerId,
                        targetRepoOwnerLogin,
                        targetRepoOwnerName,
                        targetRepoOwnerAvatar,
                        targetRepoOwnerIsOrg))
                .targetFile(value(row, targetFile, String.class))
                .targetFromLine(value(row, targetFromLine, Integer.class))
                .targetToLine(value(row, targetToLine, Integer.class))
                .sourceSnapshot(convertCommitSnapshot(row,
                        sourceSha,
                        sourceBranch,
                        sourcePullId,
                        sourceRepoId,
                        sourceRepoName,
                        sourceRepoDescription,
                        sourceRepoOwnerId,
                        sourceRepoOwnerLogin,
                        sourceRepoOwnerName,
                        sourceRepoOwnerAvatar,
                        sourceRepoOwnerIsOrg))
                .sourceFile(value(row, sourceFile, String.class))
                .sourceFromLine(value(row, sourceFromLine, Integer.class))
                .sourceToLine(value(row, sourceToLine, Integer.class))
                .build();
    }

    static <T> T value(final Row row, final String name, final Class<T> clazz) {
        return Objects.requireNonNull(row.get(name, clazz));
    }

    @Nullable
    static <T> T nullable(final Row row, @Nullable final String name, final Class<T> clazz) {
        if (name == null || NOTHING.equals(name)) {
            return null;
        }
        return row.get(name, clazz);
    }

    static Integer integer(final Row row, final String name) {
        return or(row.get(name, Integer.class), 0);
    }

    private static <T> T or(@Nullable final T value, final T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
