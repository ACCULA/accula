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
                                .bind("$1", commit.getSha())
                                .execute())
                        .flatMap(PostgresqlResult::getRowsUpdated)
                        .filter(Integer.valueOf(1)::equals)
                        .flatMap(rowsUpdated -> Repos.closeAndReturn(connection, commit)));
    }

    @Override
    public Flux<Commit> upsert(final Collection<Commit> commits) {
        if (commits.isEmpty()) {
            return Flux.empty();
        }

        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = insertStatement(connection);
                    commits.forEach(commit -> statement
                            .bind("$1", commit.getSha())
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
                    shas.forEach(sha -> statement
                            .bind("$1", sha)
                            .add());
                    statement.fetchSize(shas.size());

                    return Repos.convertMany(statement.execute(), connection, this::convert);
                });
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO commit (sha) " +
                                 "VALUES ($1) " +
                                 "ON CONFLICT DO NOTHING");
        //@formatter:on
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("SELECT * " +
                                 "FROM commit");
        //@formatter:on
    }

    private Commit convert(final Row row) {
        return Converters.convertCommit(row, "commit_sha");
    }
}
