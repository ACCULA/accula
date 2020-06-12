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

import java.time.Instant;
import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullRepoImpl implements PullRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Mono<Pull> upsert(final Pull pull) {
        return withConnection(connection -> applyInsertBindings(insertStatement(connection), pull)
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then(Mono.just(pull)));
    }

    @Override
    public Flux<Pull> upsert(final Collection<Pull> pulls) {
        if (pulls.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = insertStatement(connection);
            pulls.forEach(pull -> applyInsertBindings(statement, pull).add());
//            statement.fetchSize(pulls.size());

            return statement.execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(pulls));
        });
    }

    @Override
    public Mono<Pull> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(selectByIdStatement(connection)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public Flux<Pull> findById(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = selectByIdStatement(connection);
            ids.forEach(id -> statement
                    .bind("$1", id)
                    .add());
//            statement.fetchSize(ids.size());

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert));
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

    private static PostgresqlStatement insertStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO pull (id,\n" +
                                 "                  number,\n" +
                                 "                  title,\n" +
                                 "                  open,\n" +
                                 "                  created_at,\n" +
                                 "                  updated_at,\n" +
                                 "                  head_commit_snapshot_sha,\n" +
                                 "                  head_commit_snapshot_repo_id,\n" +
                                 "                  base_commit_snapshot_sha,\n" +
                                 "                  base_commit_snapshot_repo_id,\n" +
                                 "                  project_id,\n" +
                                 "                  author_github_id)\n" +
                                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)\n" +
                                 "ON CONFLICT (id) DO UPDATE\n" +
                                 "   SET title = $3,\n" +
                                 "       open = $4,\n" +
                                 "       updated_at = $6,\n" +
                                 "       head_commit_snapshot_sha = $7,\n" +
                                 "       base_commit_snapshot_sha = $9\n");
    }

    private static PostgresqlStatement applyInsertBindings(final PostgresqlStatement statement, final Pull pull) {
        return statement
                .bind("$1", pull.getId())
                .bind("$2", pull.getNumber())
                .bind("$3", pull.getTitle())
                .bind("$4", pull.isOpen())
                .bind("$5", pull.getCreatedAt())
                .bind("$6", pull.getUpdatedAt())
                .bind("$7", pull.getHead().getSha())
                .bind("$8", pull.getHead().getRepo().getId())
                .bind("$9", pull.getBase().getSha())
                .bind("$10", pull.getBase().getRepo().getId())
                .bind("$11", pull.getProjectId())
                .bind("$12", pull.getAuthor().getId());
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, "WHERE pull.id = $1");
    }

    private static PostgresqlStatement selectByProjectIdStatement(final Connection connection) {
        return selectStatement(connection, "WHERE pull.project_id = $1\n" +
                                           "ORDER BY pull.updated_at DESC\n");
    }

    private static PostgresqlStatement selectByNumberStatement(final Connection connection) {
        return selectStatement(connection, "WHERE pull.project_id = $1 AND pull.number = $2");
    }

    private static PostgresqlStatement selectUpdatedEarlierStatement(final Connection connection) {
        return selectStatement(connection, "WHERE pull.project_id = $1 AND\n" +
                                           "      pull.number != $2 AND\n" +
                                           "      pull.updated_at <= (SELECT updated_at\n" +
                                           "                          FROM pull\n" +
                                           "                          WHERE project_id = $1 AND number = $2)\n");
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String whereClause) {
        @Language("SQL") final var sql = "SELECT pull.id                AS id,\n" +
                                         "       pull.number            AS number,\n" +
                                         "       pull.title             AS title,\n" +
                                         "       pull.open              AS open,\n" +
                                         "       pull.created_at        AS created_at,\n" +
                                         "       pull.updated_at        AS updated_at,\n" +
                                         "       pull.project_id        AS project_id,\n" +
                                         "       head_snap.sha          AS head_snap_sha,\n" +
                                         "       head_snap.branch       AS head_snap_branch,\n" +
                                         "       head_repo.id           AS head_repo_id,\n" +
                                         "       head_repo.name         AS head_repo_name,\n" +
                                         "       head_repo.description  AS head_repo_description,\n" +
                                         "       head_repo_owner.id     AS head_repo_owner_id,\n" +
                                         "       head_repo_owner.login  AS head_repo_owner_login,\n" +
                                         "       head_repo_owner.name   AS head_repo_owner_name,\n" +
                                         "       head_repo_owner.avatar AS head_repo_owner_avatar,\n" +
                                         "       head_repo_owner.is_org AS head_repo_owner_is_org,\n" +
                                         "       base_snap.sha          AS base_snap_sha,\n" +
                                         "       base_snap.branch       AS base_snap_branch,\n" +
                                         "       base_repo.id           AS base_repo_id,\n" +
                                         "       base_repo.name         AS base_repo_name,\n" +
                                         "       base_repo.description  AS base_repo_description,\n" +
                                         "       base_repo_owner.id     AS base_repo_owner_id,\n" +
                                         "       base_repo_owner.login  AS base_repo_owner_login,\n" +
                                         "       base_repo_owner.name   AS base_repo_owner_name,\n" +
                                         "       base_repo_owner.avatar AS base_repo_owner_avatar,\n" +
                                         "       base_repo_owner.is_org AS base_repo_owner_is_org,\n" +
                                         "       author.id              AS author_id,\n" +
                                         "       author.login           AS author_login,\n" +
                                         "       author.name            AS author_name,\n" +
                                         "       author.avatar          AS author_avatar,\n" +
                                         "       author.is_org          AS author_is_org\n" +
                                         "FROM pull\n" +
                                         "   JOIN commit_snapshot head_snap\n" +
                                         "       ON pull.head_commit_snapshot_sha = head_snap.sha\n" +
                                         "   JOIN repo_github base_repo\n" +
                                         "       ON pull.base_commit_snapshot_repo_id = base_repo.id\n" +
                                         "   JOIN user_github base_repo_owner\n" +
                                         "       ON base_repo.owner_id = base_repo_owner.id\n" +
                                         "   JOIN commit_snapshot base_snap\n" +
                                         "       ON pull.base_commit_snapshot_sha = base_snap.sha\n" +
                                         "   JOIN repo_github head_repo\n" +
                                         "       ON pull.head_commit_snapshot_repo_id = head_repo.id\n" +
                                         "   JOIN user_github head_repo_owner\n" +
                                         "       ON head_repo.owner_id = head_repo_owner.id\n" +
                                         "   JOIN user_github author\n" +
                                         "       ON pull.author_github_id = author.id\n";
        return (PostgresqlStatement) connection.createStatement(String.format("%s %s", sql, whereClause));
    }

    private Pull convert(final Row row) {
        return Pull.builder()
                .id(Converters.value(row, "id", Long.class))
                .number(Converters.value(row, "number", Integer.class))
                .title(Converters.value(row, "title", String.class))
                .open(Converters.value(row, "open", Boolean.class))
                .createdAt(Converters.value(row, "created_at", Instant.class))
                .updatedAt(Converters.value(row, "updated_at", Instant.class))
                .head(Converters.convertCommitSnapshot(row,
                        "head_snap_sha",
                        "head_snap_branch",
                        Converters.NOTHING,
                        "head_repo_id",
                        "head_repo_name",
                        "head_repo_description",
                        "head_repo_owner_id",
                        "head_repo_owner_login",
                        "head_repo_owner_name",
                        "head_repo_owner_avatar",
                        "head_repo_owner_is_org"))
                .base(Converters.convertCommitSnapshot(row,
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
                        "base_repo_owner_is_org"))
                .author(Converters.convertUser(row,
                        "author_id",
                        "author_login",
                        "author_name",
                        "author_avatar",
                        "author_is_org"))
                .projectId(Converters.value(row, "project_id", Long.class))
                .build();
    }
}
