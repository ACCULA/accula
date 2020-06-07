package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
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
public final class PullRepoImpl implements PullRepo {
    private final ConnectionPool connectionPool;

    @Override
    public Mono<Pull> upsert(final Pull pull) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(applyInsertBindings(insertStatement(connection), pull)
                                .execute())
                        .flatMap(PostgresqlResult::getRowsUpdated)
                        .filter(Integer.valueOf(1)::equals)
                        .flatMap(rowsUpdated -> Repos.closeAndReturn(connection, pull)));
    }

    @Override
    public Flux<Pull> upsert(final Collection<Pull> pulls) {
        if (pulls.isEmpty()) {
            return Flux.empty();
        }

        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = insertStatement(connection);
                    pulls.forEach(pull -> applyInsertBindings(statement, pull).add());
                    statement.fetchSize(pulls.size());

                    return statement.execute()
                            .flatMap(PostgresqlResult::getRowsUpdated)
                            .filter(Integer.valueOf(pulls.size())::equals)
                            .thenMany(Repos.closeAndReturn(connection, pulls));
                });
    }

    @Override
    public Mono<Pull> findById(final Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(selectByIdStatement(connection)
                                .bind("$1", id)
                                .execute())
                        .flatMap(result -> Repos.convert(result, connection, this::convert)));
    }

    @Override
    public Flux<Pull> findById(final Collection<Long> ids) {
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = selectByIdStatement(connection);
                    ids.forEach(id -> statement
                            .bind("$1", id)
                            .add());
                    statement.fetchSize(ids.size());

                    return statement.execute()
                            .flatMap(result -> Repos.convert(result, connection, this::convert));
                });
    }

    @Override
    public Flux<Pull> findByProjectId(final Long projectId) {
        return connectionPool
                .create()
                .flatMapMany(connection -> Mono
                        .from(selectByProjectIdStatement(connection)
                                .bind("$1", projectId)
                                .execute())
                        .flatMapMany(result -> Repos.convertMany(result, connection, this::convert)));
    }

    @Override
    public Mono<Integer> countOpenOnes(final Long projectId) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(selectCountStatement(connection)
                                .bind("$1", projectId)
                                .execute())
                        .flatMap(result -> Repos.column(result, "count", Integer.class, connection)));
    }

    @Override
    public Flux<Integer> countOpenOnes(final Collection<Long> projectIds) {
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = selectCountStatement(connection);
                    projectIds.forEach(projectId -> statement
                            .bind("$1", projectId)
                            .add());
                    statement.fetchSize(projectIds.size());

                    return Repos.convertMany(statement.execute(), connection, row -> Converters.value(row, "count", Integer.class));
                });
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO pull (id," +
                                 "                  number," +
                                 "                  title," +
                                 "                  open," +
                                 "                  created_at," +
                                 "                  updated_at," +
                                 "                  head_last_commit_sha," +
                                 "                  head_branch," +
                                 "                  head_repo_id," +
                                 "                  base_last_commit_sha," +
                                 "                  base_branch," +
                                 "                  base_repo_id, " +
                                 "                  project_id," +
                                 "                  author_github_id) " +
                                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14) " +
                                 "ON CONFLICT (id) DO UPDATE " +
                                 "   SET title = $3," +
                                 "       open = $4," +
                                 "       updated_at = $6," +
                                 "       head_last_commit_sha = $7," +
                                 "       base_last_commit_sha = $10," +
                                 "       base_branch = $11");
        //@formatter:on
    }

    private static PostgresqlStatement applyInsertBindings(final PostgresqlStatement statement, final Pull pull) {
        return statement
                .bind("$1", pull.getId())
                .bind("$2", pull.getNumber())
                .bind("$3", pull.getTitle())
                .bind("$4", pull.isOpen())
                .bind("$5", pull.getCreatedAt())
                .bind("$6", pull.getUpdatedAt())
                .bind("$7", pull.getHead().getCommit().getSha())
                .bind("$8", pull.getHead().getBranch())
                .bind("$9", pull.getHead().getRepo().getId())
                .bind("$10", pull.getBase().getCommit().getSha())
                .bind("$11", pull.getBase().getBranch())
                .bind("$12", pull.getBase().getRepo().getId())
                .bind("$13", pull.getProjectId())
                .bind("$14", pull.getAuthor().getId());
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, "id");
    }

    private static PostgresqlStatement selectByProjectIdStatement(final Connection connection) {
        return selectStatement(connection, "project_id");
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String whereClauseKey) {
        //@formatter:off
        @Language("SQL")
        final var format = "SELECT pull.id                   AS id," +
                           "       pull.number               AS number," +
                           "       pull.title                AS title," +
                           "       pull.open                 AS open," +
                           "       pull.created_at           AS created_at," +
                           "       pull.updated_at           AS updated_at," +
                           "       pull.project_id           AS project_id," +
                           "       pull.head_last_commit_sha AS head_last_commit_sha," +
                           "       pull.head_branch          AS head_branch," +
                           "       head_repo.id              AS head_repo_id," +
                           "       head_repo.name            AS head_repo_name," +
                           "       head_repo.description     AS head_repo_description," +
                           "       head_repo_owner.id        AS head_repo_owner_id," +
                           "       head_repo_owner.login     AS head_repo_owner_login," +
                           "       head_repo_owner.name      AS head_repo_owner_name," +
                           "       head_repo_owner.avatar    AS head_repo_owner_avatar," +
                           "       head_repo_owner.is_org    AS head_repo_owner_is_org," +
                           "       base_repo.id              AS base_repo_id," +
                           "       base_repo.name            AS base_repo_name," +
                           "       base_repo.description     AS base_repo_description," +
                           "       base_repo_owner.id        AS base_repo_owner_id," +
                           "       base_repo_owner.login     AS base_repo_owner_login," +
                           "       base_repo_owner.name      AS base_repo_owner_name," +
                           "       base_repo_owner.avatar    AS base_repo_owner_avatar," +
                           "       base_repo_owner.is_org    AS base_repo_owner_is_org," +
                           "       author.id                 AS author_id," +
                           "       author.login              AS author_login," +
                           "       author.name               AS author_name," +
                           "       author.avatar             AS author_avatar," +
                           "       author.is_org             AS author_is_org " +
                           "FROM pull" +
                           "   JOIN user_github author" +
                           "       ON pull.author_github_id = author.id" +
                           "   JOIN repo_github base_repo" +
                           "       ON pull.base_repo_id = base_repo.id" +
                           "   JOIN user_github base_repo_owner" +
                           "       ON base_repo.owner_id = base_repo_owner" +
                           "   JOIN repo_github head_repo" +
                           "       ON pull.head_repo_id = head_repo.id" +
                           "   JOIN user_github head_repo_owner" +
                           "       ON head_repo.owner_id = head_repo_owner.id" +
                           "   JOIN user_github author" +
                           "       ON pull.author_github_id = author.id " +
                           "WHERE pull.%s = $1"; // <- String param here
        //@formatter:on
        return (PostgresqlStatement) connection.createStatement(String.format(format, whereClauseKey));
    }

    private static PostgresqlStatement selectCountStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("SELECT count(*) FROM pull WHERE project_id = $1 AND open");
    }

    private Pull convert(final Row row) {
        return Pull.builder()
                .id(Converters.value(row, "id", Long.class))
                .number(Converters.value(row, "number", Long.class))
                .title(Converters.value(row, "title", String.class))
                .open(Converters.value(row, "open", Boolean.class))
                .createdAt(Converters.value(row, "created_at", Instant.class))
                .updatedAt(Converters.value(row, "updated_at", Instant.class))
                .head(Converters.convertPullMarker(row,
                        "head_last_commit_sha",
                        "head_branch",
                        "head_repo_id",
                        "head_repo_name",
                        "head_repo_description",
                        "head_repo_owner_id",
                        "head_repo_owner_login",
                        "head_repo_owner_name",
                        "head_repo_owner_avatar",
                        "head_repo_owner_is_org"))
                .base(Converters.convertPullMarker(row,
                        "base_last_commit_sha",
                        "base_branch",
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
