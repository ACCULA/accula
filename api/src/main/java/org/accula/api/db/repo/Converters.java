package org.accula.api.db.repo;

import io.r2dbc.spi.Row;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * @author Anton Lamtev
 */
@SuppressWarnings({
    "PMD.ConfusingTernary",
    "PMD.UseObjectForClearerAPI",
    "PMD.ExcessiveParameterList",
    "SameParameterValue",
})
final class Converters {
    static final String NOTHING = "";
    static final String EMPTY_CLAUSE = NOTHING;
    static final char SPACE = ' ';

    private Converters() {
    }

    static GithubUser convertUser(final Row row,
                                  final String id,
                                  final String login,
                                  final String name,
                                  final String avatar,
                                  final String organization) {
        return GithubUser.builder()
            .id(longInteger(row, id))
            .login(string(row, login))
            .name(nullable(row, name, String.class))
            .avatar(string(row, avatar))
            .isOrganization(bool(row, organization))
            .build();
    }

    static GithubRepo convertRepo(final Row row,
                                  final String id,
                                  final String name,
                                  final String isPrivate,
                                  final String description,
                                  final String ownerId,
                                  final String ownerLogin,
                                  final String ownerName,
                                  final String ownerAvatar,
                                  final String ownerOrganization) {
        return GithubRepo.builder()
            .id(longInteger(row, id))
            .name(string(row, name))
            .isPrivate(bool(row, isPrivate))
            .description(string(row, description))
            .owner(convertUser(row, ownerId, ownerLogin, ownerName, ownerAvatar, ownerOrganization))
            .build();
    }

    static Commit convertCommit(final Row row,
                                final String sha,
                                final String isMerge,
                                final String authorName,
                                final String authorEmail,
                                final String date) {
        return Commit.builder()
                .sha(string(row, sha))
                .isMerge(bool(row, isMerge))
                .authorName(string(row, authorName))
                .authorEmail(string(row, authorEmail))
                .date(value(row, date, Instant.class))
                .build();
    }

    @Nullable
    static Snapshot.PullInfo convertSnapshotPullInfo(final Row row,
                                                     final String pullId,
                                                     final String pullNumber) {
        if (NOTHING.equals(pullId) || NOTHING.equals(pullNumber)) {
            return null;
        }
        return new Snapshot.PullInfo(longInteger(row, pullId), integer(row, pullNumber));
    }

    static Snapshot convertSnapshot(final Row row,
                                    final String commitSha,
                                    final String commitIsMerge,
                                    final String commitAuthorName,
                                    final String commitAuthorEmail,
                                    final String commitDate,
                                    final String branch,
                                    final String pullId,
                                    final String pullNumber,
                                    final String repoId,
                                    final String repoName,
                                    final String repoIsPrivate,
                                    final String repoDescription,
                                    final String repoOwnerId,
                                    final String repoOwnerLogin,
                                    final String repoOwnerName,
                                    final String repoOwnerAvatar,
                                    final String repoOwnerOrganization) {
        return Snapshot.builder()
                .commit(convertCommit(row,
                        commitSha,
                        commitIsMerge,
                        commitAuthorName,
                        commitAuthorEmail,
                        commitDate))
                .branch(string(row, branch))
                .pullInfo(convertSnapshotPullInfo(row, pullId, pullNumber))
                .repo(convertRepo(row,
                        repoId,
                        repoName,
                        repoIsPrivate,
                        repoDescription,
                        repoOwnerId,
                        repoOwnerLogin,
                        repoOwnerName,
                        repoOwnerAvatar,
                        repoOwnerOrganization))
                .build();
    }

    static User convertUser(final Row row,
                            final String id,
                            final String accessToken,
                            final String githubId,
                            final String githubLogin,
                            final String githubName,
                            final String githubAvatar,
                            final String githubOrganization,
                            final String role) {
        return new User(
                longInteger(row, id),
                string(row, accessToken),
                convertUser(row, githubId, githubLogin, githubName, githubAvatar, githubOrganization),
                value(row, role, User.Role.class)
        );
    }

    static Clone convertClone(final Row row,
                              final String id,
                              final String targetSnippetId,
                              final String targetCommitSha,
                              final String targetCommitIsMerge,
                              final String targetCommitAuthorName,
                              final String targetCommitAuthorEmail,
                              final String targetCommitDate,
                              final String targetBranch,
                              final String targetPullId,
                              final String targetPullNumber,
                              final String targetRepoId,
                              final String targetRepoName,
                              final String targetRepoIsPrivate,
                              final String targetRepoDescription,
                              final String targetRepoOwnerId,
                              final String targetRepoOwnerLogin,
                              final String targetRepoOwnerName,
                              final String targetRepoOwnerAvatar,
                              final String targetRepoOwnerIsOrg,
                              final String targetFile,
                              final String targetFromLine,
                              final String targetToLine,
                              final String sourceSnippetId,
                              final String sourceCommitSha,
                              final String sourceCommitIsMerge,
                              final String sourceCommitAuthorName,
                              final String sourceCommitAuthorEmail,
                              final String sourceCommitDate,
                              final String sourceBranch,
                              final String sourcePullId,
                              final String sourcePullNumber,
                              final String sourceRepoId,
                              final String sourceRepoName,
                              final String sourceRepoIsPrivate,
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
                .id(longInteger(row, id))
                .target(convertCloneSnippet(row,
                        targetSnippetId,
                        targetCommitSha,
                        targetCommitIsMerge,
                        targetCommitAuthorName,
                        targetCommitAuthorEmail,
                        targetCommitDate,
                        targetBranch,
                        targetPullId,
                        targetPullNumber,
                        targetRepoId,
                        targetRepoName,
                        targetRepoIsPrivate,
                        targetRepoDescription,
                        targetRepoOwnerId,
                        targetRepoOwnerLogin,
                        targetRepoOwnerName,
                        targetRepoOwnerAvatar,
                        targetRepoOwnerIsOrg,
                        targetFile,
                        targetFromLine,
                        targetToLine))
                .source(convertCloneSnippet(row,
                        sourceSnippetId,
                        sourceCommitSha,
                        sourceCommitIsMerge,
                        sourceCommitAuthorName,
                        sourceCommitAuthorEmail,
                        sourceCommitDate,
                        sourceBranch,
                        sourcePullId,
                        sourcePullNumber,
                        sourceRepoId,
                        sourceRepoName,
                        sourceRepoIsPrivate,
                        sourceRepoDescription,
                        sourceRepoOwnerId,
                        sourceRepoOwnerLogin,
                        sourceRepoOwnerName,
                        sourceRepoOwnerAvatar,
                        sourceRepoOwnerIsOrg,
                        sourceFile,
                        sourceFromLine,
                        sourceToLine))
                .build();
    }

    static Clone.Snippet convertCloneSnippet(final Row row,
                                             final String id,
                                             final String commitSha,
                                             final String commitIsMerge,
                                             final String commitAuthorName,
                                             final String commitAuthorEmail,
                                             final String commitDate,
                                             final String branch,
                                             final String pullId,
                                             final String pullNumber,
                                             final String repoId,
                                             final String repoName,
                                             final String repoIsPrivate,
                                             final String repoDescription,
                                             final String repoOwnerId,
                                             final String repoOwnerLogin,
                                             final String repoOwnerName,
                                             final String repoOwnerAvatar,
                                             final String repoOwnerIsOrg,
                                             final String file,
                                             final String fromLine,
                                             final String toLine) {
        return Clone.Snippet.builder()
                .id(longInteger(row, id))
                .snapshot(convertSnapshot(row,
                        commitSha,
                        commitIsMerge,
                        commitAuthorName,
                        commitAuthorEmail,
                        commitDate,
                        branch,
                        pullId,
                        pullNumber,
                        repoId,
                        repoName,
                        repoIsPrivate,
                        repoDescription,
                        repoOwnerId,
                        repoOwnerLogin,
                        repoOwnerName,
                        repoOwnerAvatar,
                        repoOwnerIsOrg))
                .file(string(row, file))
                .fromLine(integer(row, fromLine))
                .toLine(integer(row, toLine))
                .build();
    }

    static Pull convertPull(final Row row,
                            final String id,
                            final String number,
                            final String title,
                            final String open,
                            final String createdAt,
                            final String updatedAt,
                            final String headCommitSha,
                            final String headCommitIsMerge,
                            final String headCommitAuthorName,
                            final String headCommitAuthorEmail,
                            final String headCommitDate,
                            final String headSnapBranch,
                            final String headSnapPullId,
                            final String headSnapPullNumber,
                            final String headRepoId,
                            final String headRepoName,
                            final String headRepoIsPrivate,
                            final String headRepoDescription,
                            final String headRepoOwnerId,
                            final String headRepoOwnerLogin,
                            final String headRepoOwnerName,
                            final String headRepoOwnerAvatar,
                            final String headRepoOwnerIsOrg,
                            final String baseCommitSha,
                            final String baseCommitIsMerge,
                            final String baseCommitAuthorName,
                            final String baseCommitAuthorEmail,
                            final String baseCommitDate,
                            final String baseSnapBranch,
                            final String baseRepoId,
                            final String baseRepoName,
                            final String baseRepoIsPrivate,
                            final String baseRepoDescription,
                            final String baseRepoOwnerId,
                            final String baseRepoOwnerLogin,
                            final String baseRepoOwnerName,
                            final String baseRepoOwnerAvatar,
                            final String baseRepoOwnerOrganization,
                            final String authorId,
                            final String authorLogin,
                            final String authorName,
                            final String authorAvatar,
                            final String authorIsOrg,
                            final String primaryProjectId) {
        return Pull.builder()
                .id(longInteger(row, id))
                .number(integer(row, number))
                .title(string(row, title))
                .isOpen(bool(row, open))
                .createdAt(value(row, createdAt, Instant.class))
                .updatedAt(value(row, updatedAt, Instant.class))
                .head(convertSnapshot(row,
                        headCommitSha,
                        headCommitIsMerge,
                        headCommitAuthorName,
                        headCommitAuthorEmail,
                        headCommitDate,
                        headSnapBranch,
                        headSnapPullId,
                        headSnapPullNumber,
                        headRepoId,
                        headRepoName,
                        headRepoIsPrivate,
                        headRepoDescription,
                        headRepoOwnerId,
                        headRepoOwnerLogin,
                        headRepoOwnerName,
                        headRepoOwnerAvatar,
                        headRepoOwnerIsOrg))
                .base(convertSnapshot(row,
                        baseCommitSha,
                        baseCommitIsMerge,
                        baseCommitAuthorName,
                        baseCommitAuthorEmail,
                        baseCommitDate,
                        baseSnapBranch,
                        NOTHING,
                        NOTHING,
                        baseRepoId,
                        baseRepoName,
                        baseRepoIsPrivate,
                        baseRepoDescription,
                        baseRepoOwnerId,
                        baseRepoOwnerLogin,
                        baseRepoOwnerName,
                        baseRepoOwnerAvatar,
                        baseRepoOwnerOrganization))
                .author(convertUser(row,
                        authorId,
                        authorLogin,
                        authorName,
                        authorAvatar,
                        authorIsOrg))
                .primaryProjectId(nullable(row, primaryProjectId, Long.class))
                .build();
    }

    static String string(final Row row, final String name) {
        return value(row, name, String.class);
    }

    static <T> T value(final Row row, final String name, final Class<T> clazz) {
        return Checks.notNull(row.get(name, clazz), name);
    }

    @Nullable
    static <T> T nullable(final Row row, final String name, final Class<T> clazz) {
        if (NOTHING.equals(name)) {
            return null;
        }
        return row.get(name, clazz);
    }

    static Boolean bool(final Row row, final String name) {
        return Checks.notNull(row.get(name, Boolean.class), name);
    }

    static Integer integer(final Row row, final String name, final Integer fallback) {
        return or(row.get(name, Integer.class), fallback);
    }

    static Integer integer(final Row row, final String name) {
        return Checks.notNull(row.get(name, Integer.class), name);
    }

    static Long longInteger(final Row row, final String name) {
        return Checks.notNull(row.get(name, Long.class), name);
    }

    static List<Long> ids(final Row row, final String name) {
        final var ids = row.get(name, Long[].class);
        return ids != null ? List.of(ids) : List.of();
    }

    static List<String> strings(final Row row, final String name) {
        final var strings = row.get(name, String[].class);
        return strings != null ? List.of(strings) : List.of();
    }

    private static <T> T or(@Nullable final T value, final T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
