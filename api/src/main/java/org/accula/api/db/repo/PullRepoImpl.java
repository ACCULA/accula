package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Pull;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullRepoImpl implements PullRepo, ConnectionProvidedRepo {
    private static final String EMPTY_CLAUSE = "";
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<Pull> upsert(final Collection<Pull> pulls) {
        if (pulls.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO pull (id,
                                      number,
                                      title,
                                      open,
                                      created_at,
                                      updated_at,
                                      head_snapshot_sha,
                                      head_snapshot_repo_id,
                                      base_snapshot_sha,
                                      base_snapshot_repo_id,
                                      project_id,
                                      author_github_id)
                    VALUES ($collection)
                    ON CONFLICT (id) DO UPDATE
                       SET title = excluded.title,
                           open = excluded.open,
                           updated_at = excluded.updated_at,
                           head_snapshot_sha = excluded.head_snapshot_sha,
                           base_snapshot_sha = excluded.base_snapshot_sha
                    """);
            statement.bind(pulls, pull -> new Object[]{
                    pull.getId(),
                    pull.getNumber(),
                    pull.getTitle(),
                    pull.isOpen(),
                    pull.getCreatedAt(),
                    pull.getUpdatedAt(),
                    pull.getHead().getSha(),
                    pull.getHead().getRepo().getId(),
                    pull.getBase().getSha(),
                    pull.getBase().getRepo().getId(),
                    pull.getProjectId(),
                    pull.getAuthor().getId()
            });

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
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
                    .flatMap(result -> ConnectionProvidedRepo.convertMany(result, this::convert));
        });
    }

    @Override
    public Mono<Pull> findByNumber(final Long projectId, final Integer number) {
        return withConnection(connection -> Mono
                .from(selectByNumberStatement(connection)
                        .bind("$1", projectId)
                        .bind("$2", number)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public Flux<Pull> findPrevious(Long projectId, Integer number, Long authorId) {
        return manyWithConnection(connection -> Mono
                .from(selectPreviousByNumberAndAuthorIdStatement(connection)
                        .bind("$1", projectId)
                        .bind("$2", number)
                        .bind("$3", authorId)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    @Override
    public Flux<Pull> findUpdatedEarlierThan(final Long projectId, final Integer number) {
        return manyWithConnection(connection -> Mono
                .from(selectUpdatedEarlierStatement(connection)
                        .bind("$1", projectId)
                        .bind("$2", number)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    @Override
    public Flux<Pull> findByProjectId(final Long projectId) {
        return manyWithConnection(connection -> Mono
                .from(selectByProjectIdStatement(connection)
                        .bind("$1", projectId)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    @Override
    public Flux<Integer> numbersByIds(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = (PostgresqlStatement) connection.createStatement("""
                    SELECT number 
                    FROM pull
                        JOIN unnest($1) WITH ORDINALITY AS arr(id, ord)
                            ON pull.id = arr.id
                    ORDER BY arr.ord
                    """);
            statement.bind("$1", ids.toArray(new Long[0]));

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.columnFlux(result, "number", Integer.class));
        });
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
                WHERE pull.project_id = $1
                """, """
                ORDER BY pull.open DESC, pull.updated_at DESC
                """);
    }

    private static PostgresqlStatement selectByNumberStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, "WHERE pull.project_id = $1 AND pull.number = $2", EMPTY_CLAUSE);
    }

    private static PostgresqlStatement selectPreviousByNumberAndAuthorIdStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, """
                WHERE pull.project_id = $1 AND pull.number < $2 AND author.id = $3
                """, """
                ORDER BY pull.number DESC
                """);
    }

    private static PostgresqlStatement selectUpdatedEarlierStatement(final Connection connection) {
        return selectStatement(connection, EMPTY_CLAUSE, """
                WHERE pull.project_id = $1 AND
                      pull.number != $2 AND
                      pull.updated_at <= (SELECT updated_at
                                          FROM pull
                                          WHERE project_id = $1 AND number = $2)
                """, EMPTY_CLAUSE);
    }

    private static PostgresqlStatement selectStatement(final Connection connection,
                                                       final String fromClauseExtension,
                                                       final String whereClause,
                                                       final String orderByClause) {
        @Language("SQL") final var sql = """
                SELECT pull.id                AS id,
                       pull.number            AS number,
                       pull.title             AS title,
                       pull.open              AS open,
                       pull.created_at        AS created_at,
                       pull.updated_at        AS updated_at,
                       pull.project_id        AS project_id,
                       head_snap.sha          AS head_snap_sha,
                       head_snap.branch       AS head_snap_branch,
                       head_repo.id           AS head_repo_id,
                       head_repo.name         AS head_repo_name,
                       head_repo.description  AS head_repo_description,
                       head_repo_owner.id     AS head_repo_owner_id,
                       head_repo_owner.login  AS head_repo_owner_login,
                       head_repo_owner.name   AS head_repo_owner_name,
                       head_repo_owner.avatar AS head_repo_owner_avatar,
                       head_repo_owner.is_org AS head_repo_owner_is_org,
                       base_snap.sha          AS base_snap_sha,
                       base_snap.branch       AS base_snap_branch,
                       base_repo.id           AS base_repo_id,
                       base_repo.name         AS base_repo_name,
                       base_repo.description  AS base_repo_description,
                       base_repo_owner.id     AS base_repo_owner_id,
                       base_repo_owner.login  AS base_repo_owner_login,
                       base_repo_owner.name   AS base_repo_owner_name,
                       base_repo_owner.avatar AS base_repo_owner_avatar,
                       base_repo_owner.is_org AS base_repo_owner_is_org,
                       author.id              AS author_id,
                       author.login           AS author_login,
                       author.name            AS author_name,
                       author.avatar          AS author_avatar,
                       author.is_org          AS author_is_org
                FROM pull
                   JOIN snapshot head_snap
                       ON pull.head_snapshot_sha = head_snap.sha
                   JOIN repo_github base_repo
                       ON pull.base_snapshot_repo_id = base_repo.id
                   JOIN user_github base_repo_owner
                       ON base_repo.owner_id = base_repo_owner.id
                   JOIN snapshot base_snap
                       ON pull.base_snapshot_sha = base_snap.sha
                   JOIN repo_github head_repo
                       ON pull.head_snapshot_repo_id = head_repo.id
                   JOIN user_github head_repo_owner
                       ON head_repo.owner_id = head_repo_owner.id
                   JOIN user_github author
                       ON pull.author_github_id = author.id
                """;
        return (PostgresqlStatement) connection
                .createStatement(String.format("%s %s %s %s", sql, fromClauseExtension, whereClause, orderByClause));
    }

    private Pull convert(final Row row) {
        return Converters.convertPull(row,
                "id",
                "number",
                "title",
                "open",
                "created_at",
                "updated_at",
                "head_snap_sha",
                "head_snap_branch",
                "id",
                "head_repo_id",
                "head_repo_name",
                "head_repo_description",
                "head_repo_owner_id",
                "head_repo_owner_login",
                "head_repo_owner_name",
                "head_repo_owner_avatar",
                "head_repo_owner_is_org",
                "base_snap_sha",
                "base_snap_branch",
                Converters.NOTHING,
                "base_repo_id",
                "base_repo_name",
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
                "project_id");
    }
}
