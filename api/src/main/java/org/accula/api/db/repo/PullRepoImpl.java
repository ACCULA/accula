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
                .createStatement("""
                        INSERT INTO pull (id,
                                          number,
                                          title,
                                          open,
                                          created_at,
                                          updated_at,
                                          head_commit_snapshot_sha,
                                          head_commit_snapshot_repo_id,
                                          base_commit_snapshot_sha,
                                          base_commit_snapshot_repo_id,
                                          project_id,
                                          author_github_id)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
                        ON CONFLICT (id) DO UPDATE
                           SET title = $3,
                               open = $4,
                               updated_at = $6,
                               head_commit_snapshot_sha = $7,
                               base_commit_snapshot_sha = $9
                        """);
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
        return selectStatement(connection, """
                WHERE pull.project_id = $1
                ORDER BY pull.updated_at DESC
                """);
    }

    private static PostgresqlStatement selectByNumberStatement(final Connection connection) {
        return selectStatement(connection, "WHERE pull.project_id = $1 AND pull.number = $2");
    }

    private static PostgresqlStatement selectUpdatedEarlierStatement(final Connection connection) {
        return selectStatement(connection, """
                WHERE pull.project_id = $1 AND
                      pull.number != $2 AND
                      pull.updated_at <= (SELECT updated_at
                                          FROM pull
                                          WHERE project_id = $1 AND number = $2)
                """);
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String whereClause) {
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
                   JOIN commit_snapshot head_snap
                       ON pull.head_commit_snapshot_sha = head_snap.sha
                   JOIN repo_github base_repo
                       ON pull.base_commit_snapshot_repo_id = base_repo.id
                   JOIN user_github base_repo_owner
                       ON base_repo.owner_id = base_repo_owner.id
                   JOIN commit_snapshot base_snap
                       ON pull.base_commit_snapshot_sha = base_snap.sha
                   JOIN repo_github head_repo
                       ON pull.head_commit_snapshot_repo_id = head_repo.id
                   JOIN user_github head_repo_owner
                       ON head_repo.owner_id = head_repo_owner.id
                   JOIN user_github author
                       ON pull.author_github_id = author.id
                """;
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
