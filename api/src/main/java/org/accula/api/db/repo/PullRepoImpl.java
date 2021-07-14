package org.accula.api.db.repo;

import com.google.common.collect.Iterables;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.PullSnapshots;
import org.intellij.lang.annotations.Language;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.accula.api.db.repo.ConnectionProvidedRepo.convertMany;
import static org.accula.api.db.repo.Converters.EMPTY_CLAUSE;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullRepoImpl implements PullRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<Pull> upsert(final Collection<Pull> pulls) {
        if (pulls.isEmpty()) {
            return Flux.empty();
        }

        return transactionalMany(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO pull (id,
                                      number,
                                      title,
                                      open,
                                      created_at,
                                      updated_at,
                                      head_snapshot_sha,
                                      head_snapshot_repo_id,
                                      head_snapshot_branch,
                                      base_snapshot_sha,
                                      base_snapshot_repo_id,
                                      base_snapshot_branch,
                                      author_github_id)
                    VALUES ($collection)
                    ON CONFLICT (id) DO UPDATE
                       SET title = excluded.title,
                           open = excluded.open,
                           updated_at = excluded.updated_at,
                           head_snapshot_sha = excluded.head_snapshot_sha,
                           base_snapshot_sha = excluded.base_snapshot_sha
                    """);
            statement.bind(pulls, pull -> Bindings.of(
                    pull.id(),
                    pull.number(),
                    pull.title(),
                    pull.isOpen(),
                    pull.createdAt(),
                    pull.updatedAt(),
                    pull.head().sha(),
                    pull.head().repo().id(),
                    pull.head().branch(),
                    pull.base().sha(),
                    pull.base().repo().id(),
                    pull.base().branch(),
                    pull.author().id()
            ));

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .then(deleteAssignees(connection, pulls))
                    .then(insertAssignees(connection, pulls))
                    .thenMany(Flux.fromIterable(pulls));
        });
    }

    @Override
    public Flux<Pull> findById(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = selectByIdStatement(connection);
            statement.bind("$1", ids.toArray(new Long[0]));

            return statement
                    .execute()
                    .flatMap(result -> convertMany(result, PullRepoImpl::convert));
        });
    }

    @Override
    public Mono<Pull> findByNumber(final Long projectId, final Integer number) {
        return withConnection(connection -> Mono
                .from(selectByNumberStatement(connection)
                        .bind("$1", projectId)
                        .bind("$2", number)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, PullRepoImpl::convert)));
    }

    @Override
    public Flux<Pull> findPrevious(Long projectId, Integer number, Long authorId) {
        return manyWithConnection(connection -> selectPreviousByNumberAndAuthorIdStatement(connection)
            .bind("$1", projectId)
            .bind("$2", number)
            .bind("$3", authorId)
            .execute()
            .flatMap(result -> convertMany(result, PullRepoImpl::convert)));
    }

    @Override
    public Flux<Pull> findByProjectId(final Long projectId) {
        return manyWithConnection(connection -> selectByProjectIdStatement(connection)
            .bind("$1", projectId)
            .execute()
            .flatMap(result -> convertMany(result, PullRepoImpl::convert)));
    }

    @Override
    public Flux<Pull> findByProjectIdIncludingAssignees(final Long projectId) {
        return transactionalMany(connection -> selectByProjectIdStatement(connection)
            .bind("$1", projectId)
            .execute()
            .flatMap(result -> convertMany(result, PullRepoImpl::convert))
            .transform(byFetchingAssignees(connection)));
    }

    @Override
    public Flux<Pull> findByProjectIdIncludingSecondaryRepos(final Long projectId) {
        return manyWithConnection(connection -> selectByProjectIdIncludingSecondaryRepos(connection)
            .bind("$1", projectId)
            .execute())
            .flatMap(result -> convertMany(result, PullRepoImpl::convert));
    }

    @Override
    public Publisher<Void> mapSnapshots(final Iterable<PullSnapshots> pullSnapshots) {
        if (Iterables.isEmpty(pullSnapshots)) {
            return Flux.empty();
        }

        return withConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO snapshot_pull (snapshot_sha, snapshot_repo_id, snapshot_branch, pull_id)
                    VALUES ($collection)
                    ON CONFLICT (snapshot_sha, snapshot_repo_id, snapshot_branch, pull_id) DO NOTHING
                    """);
            for (final var item : pullSnapshots) {
                final var pullId = item.pull().id();
                statement.bind(item.snapshots(), commitSnapshot -> Bindings.of(
                        commitSnapshot.sha(),
                        commitSnapshot.repo().id(),
                        commitSnapshot.branch(),
                        pullId
                ));
            }

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .then();
        });
    }

    private static Mono<Void> deleteAssignees(final Connection connection, final Collection<Pull> pulls) {
        return ((PostgresqlStatement) connection.createStatement("""
            DELETE
            FROM pull_assignee
            WHERE pull_id = ANY ($1)
            """))
            .bind("$1", pulls.stream().map(Pull::id).toArray(Long[]::new))
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

    private static Mono<Void> insertAssignees(final Connection connection, final Collection<Pull> pulls) {
        final var pullIdToAssigneeIdList = pulls
            .stream()
            .flatMap(pull -> pull
                .assignees()
                .stream()
                .map(assignee -> new Object() {
                    final Long pullId = pull.id();
                    final Long assigneeId = assignee.id();
                })
            )
            .toList();

        if (pullIdToAssigneeIdList.isEmpty()) {
            return Mono.empty();
        }

        final var statement = BatchStatement.of(connection, """
            INSERT INTO pull_assignee (pull_id, assignee_id)
            VALUES ($collection)
            """);
        statement.bind(pullIdToAssigneeIdList, pullIdToAssigneeId -> Bindings.of(
            pullIdToAssigneeId.pullId,
            pullIdToAssigneeId.assigneeId
        ));
        return statement
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, """
                JOIN unnest($1) WITH ORDINALITY AS arr(id, ord)
                    ON pull.id = arr.id
                """, EMPTY_CLAUSE, """
                ORDER BY arr.ord
                """);
    }

    private static PostgresqlStatement selectByProjectIdStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, """
                WHERE primary_project.id = $1
                """, """
                ORDER BY pull.open DESC, pull.updated_at DESC
                """);
    }

    private static PostgresqlStatement selectByProjectIdIncludingSecondaryRepos(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, """
            WHERE base_repo.id IN (SELECT project_repo.repo_id
                                   FROM project_repo
                                   WHERE project_repo.project_id = $1)
                OR primary_project.id = $1
            """, EMPTY_CLAUSE);
    }

    private static PostgresqlStatement selectByNumberStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, "WHERE primary_project.id = $1 AND pull.number = $2", EMPTY_CLAUSE);
    }

    private static PostgresqlStatement selectPreviousByNumberAndAuthorIdStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, """
                WHERE primary_project.id = $1 AND pull.number < $2 AND author.id = $3
                """, """
                ORDER BY pull.number DESC
                """);
    }

    private static PostgresqlStatement selectStatement(final Connection connection,
                                                       final String fromClauseExtension,
                                                       final String whereClause,
                                                       final String orderByClause) {
        @Language("SQL") final var sql = """
                SELECT pull.id                  AS id,
                       pull.number              AS number,
                       pull.title               AS title,
                       pull.open                AS open,
                       pull.created_at          AS created_at,
                       pull.updated_at          AS updated_at,
                       primary_project.id       AS primary_project_id,
                       head_commit.sha          AS head_commit_sha,
                       head_commit.is_merge     AS head_commit_is_merge,
                       head_commit.author_name  AS head_commit_author_name,
                       head_commit.author_email AS head_commit_author_email,
                       head_commit.date         AS head_commit_date,
                       head_snap.branch         AS head_snap_branch,
                       pull.id                  AS head_snap_pull_id,
                       pull.number              AS head_snap_pull_number,
                       head_repo.id             AS head_repo_id,
                       head_repo.name           AS head_repo_name,
                       head_repo.is_private     AS head_repo_is_private,
                       head_repo.description    AS head_repo_description,
                       head_repo_owner.id       AS head_repo_owner_id,
                       head_repo_owner.login    AS head_repo_owner_login,
                       head_repo_owner.name     AS head_repo_owner_name,
                       head_repo_owner.avatar   AS head_repo_owner_avatar,
                       head_repo_owner.is_org   AS head_repo_owner_is_org,
                       base_commit.sha          AS base_commit_sha,
                       base_commit.is_merge     AS base_commit_is_merge,
                       base_commit.author_name  AS base_commit_author_name,
                       base_commit.author_email AS base_commit_author_email,
                       head_commit.date         AS base_commit_date,
                       base_snap.branch         AS base_snap_branch,
                       base_repo.id             AS base_repo_id,
                       base_repo.name           AS base_repo_name,
                       base_repo.is_private     AS base_repo_is_private,
                       base_repo.description    AS base_repo_description,
                       base_repo_owner.id       AS base_repo_owner_id,
                       base_repo_owner.login    AS base_repo_owner_login,
                       base_repo_owner.name     AS base_repo_owner_name,
                       base_repo_owner.avatar   AS base_repo_owner_avatar,
                       base_repo_owner.is_org   AS base_repo_owner_is_org,
                       author.id                AS author_id,
                       author.login             AS author_login,
                       author.name              AS author_name,
                       author.avatar            AS author_avatar,
                       author.is_org            AS author_is_org
                FROM pull
                   JOIN snapshot head_snap
                       ON pull.head_snapshot_sha = head_snap.sha
                            AND pull.head_snapshot_repo_id = head_snap.repo_id
                            AND pull.head_snapshot_branch = head_snap.branch
                   JOIN commit head_commit
                       ON head_snap.sha = head_commit.sha
                   JOIN repo_github head_repo
                       ON pull.head_snapshot_repo_id = head_repo.id
                   JOIN user_github head_repo_owner
                       ON head_repo.owner_id = head_repo_owner.id
                   JOIN snapshot base_snap
                       ON pull.base_snapshot_sha = base_snap.sha
                            AND pull.base_snapshot_repo_id = base_snap.repo_id
                            AND pull.base_snapshot_branch = base_snap.branch
                   JOIN commit base_commit
                       ON base_snap.sha = base_commit.sha
                   JOIN repo_github base_repo
                       ON pull.base_snapshot_repo_id = base_repo.id
                   JOIN user_github base_repo_owner
                       ON base_repo.owner_id = base_repo_owner.id
                   JOIN user_github author
                       ON pull.author_github_id = author.id
                   LEFT JOIN project primary_project
                       ON base_repo.id = primary_project.github_repo_id
                """;
        return (PostgresqlStatement) connection
                .createStatement(String.format("%s %s %s %s", sql, fromClauseExtension, whereClause, orderByClause));
    }

    private static Function<Flux<Pull>, Flux<Pull>> byFetchingAssignees(final Connection connection) {
        return pullFlux -> pullFlux
            .collectList()
            .flatMapMany(pulls -> ((PostgresqlStatement) connection
                .createStatement("""
                    SELECT pull_id,
                           assignee_id,
                           ug.login     AS assignee_login,
                           ug.name      AS assignee_name,
                           ug.avatar    AS assignee_avatar,
                           ug.is_org    AS assignee_is_org
                    FROM pull_assignee
                        JOIN user_github ug
                            ON ug.id = pull_assignee.assignee_id
                    WHERE pull_id = ANY ($1)
                    """))
                .bind("$1", pulls.stream().map(Pull::id).toArray(Long[]::new))
                .execute()
                .flatMap(result ->
                    convertMany(result, row -> new Object() {
                        final Long pullId = Converters.longInteger(row, "pull_id");
                        final GithubUser assignee = Converters.convertUser(row,
                            "assignee_id",
                            "assignee_login",
                            "assignee_name",
                            "assignee_avatar",
                            "assignee_is_org");
                    })
                )
                .collectMultimap(it -> it.pullId, it -> it.assignee)
                .flatMapIterable(assigneeMap -> pulls
                    .stream()
                    .map(pull -> {
                        final var assignees = assigneeMap.get(pull.id());
                        if (assignees == null || assignees.isEmpty()) {
                            return pull;
                        }
                        if (assignees instanceof List<GithubUser> list) {
                            return pull.withAssignees(list);
                        }
                        final var list = new ArrayList<GithubUser>(assignees.size());
                        list.addAll(assignees);
                        return pull.withAssignees(list);
                    })
                    .toList()));
    }

    private static Pull convert(final Row row) {
        return Converters.convertPull(row,
                "id",
                "number",
                "title",
                "open",
                "created_at",
                "updated_at",
                "head_commit_sha",
                "head_commit_is_merge",
                "head_commit_author_name",
                "head_commit_author_email",
                "head_commit_date",
                "head_snap_branch",
                "head_snap_pull_id",
                "head_snap_pull_number",
                "head_repo_id",
                "head_repo_name",
                "head_repo_is_private",
                "head_repo_description",
                "head_repo_owner_id",
                "head_repo_owner_login",
                "head_repo_owner_name",
                "head_repo_owner_avatar",
                "head_repo_owner_is_org",
                "base_commit_sha",
                "base_commit_is_merge",
                "base_commit_author_name",
                "base_commit_author_email",
                "base_commit_date",
                "base_snap_branch",
                "base_repo_id",
                "base_repo_name",
                "base_repo_is_private",
                "base_repo_description",
                "base_repo_owner_id",
                "base_repo_owner_login",
                "base_repo_owner_name",
                "base_repo_owner_avatar",
                "base_repo_owner_is_org",
                "author_id",
                "author_login",
                "author_name",
                "author_avatar",
                "author_is_org",
                "primary_project_id");
    }
}
