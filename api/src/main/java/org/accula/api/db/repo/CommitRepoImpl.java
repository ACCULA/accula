package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Commit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class CommitRepoImpl implements CommitRepo {
    private final ConnectionPool connectionPool;

    @Override
    public Mono<Commit> upsert(final Commit commit) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(insertStatement(connection)
                                //@formatter:on
                                .bind("$1", commit.getSha())
                                .bind("$2", commit.getRepo().getId())
                                .execute())
                        .flatMap(PostgresqlResult::getRowsUpdated)
                        .filter(Integer.valueOf(1)::equals)
                        .flatMap(rowsUpdated -> Repos.closeAndReturn(connection, commit)));
    }

    @Override
    public Flux<Commit> upsert(final Collection<Commit> commits) {
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    //@formatter:off
                    final var statement = insertStatement(connection);
                    //@formatter:on
                    commits.forEach(commit -> statement
                            .bind("$1", commit.getSha())
                            .bind("$2", commit.getRepo().getId())
                            .add());
                    statement.fetchSize(commits.size());

                    return statement.execute()
                            .flatMap(PostgresqlResult::getRowsUpdated)
                            .thenMany(Repos.closeAndReturn(connection, commits));
                });
    }

    @Override
    public Mono<Commit> findBySha(final String sha) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(selectStatement(connection)
                                .bind("$1", sha)
                                .execute())
                        .flatMap(result -> Repos.convert(result, connection, this::convert)));
    }

    @Override
    public Flux<Commit> findBySha(final Collection<String> shas) {
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = selectStatement(connection);
                    //@formatter:on
                    shas.forEach(sha -> statement
                            .bind("$1", sha)
                            .add());
                    statement.fetchSize(shas.size());

                    return statement.execute()
                            .flatMap(result -> Repos.convert(result, connection, this::convert));
                });
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO commit (sha, github_repo_id) " +
                                 "VALUES ($1, $2) " +
                                 "ON CONFLICT DO NOTHING");
        //@formatter:on
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("SELECT commit.sha        AS commit_sha," +
                                 "       repo.id           AS repo_id," +
                                 "       repo.name         AS repo_name," +
                                 "       repo.description  AS repo_description," +
                                 "       repo_owner.id     AS repo_owner_id," +
                                 "       repo_owner.login  AS repo_owner_login," +
                                 "       repo_owner.name   AS repo_owner_name," +
                                 "       repo_owner.avatar AS repo_owner_avatar," +
                                 "       repo_owner.is_org AS repo_owner_is_org " +
                                 "FROM commit" +
                                 "   JOIN repo_github repo" +
                                 "       ON commit.github_repo_id = repo.id" +
                                 "   JOIN user_github repo_owner" +
                                 "       ON repo.owner_id = repo_owner.id " +
                                 "WHERE commit.sha = $1");
        //@formatter:on
    }

    private Commit convert(final Row row) {
        return new Commit(
                Objects.requireNonNull(row.get("commit_sha", String.class)),
                GithubRepoRepoImpl.convert(row)
        );
    }
}
