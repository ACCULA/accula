package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Pull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = insertStatement(connection);
                    pulls.forEach(pull -> applyInsertBindings(statement, pull).add());
                    statement.fetchSize(pulls.size());

                    return statement.execute()
                            .flatMap(result -> Repos.convert(result, connection, this::convert));
                });
    }

    @Override
    public Mono<Pull> findById(final Long id) {
        return Mono.empty();
    }

    @Override
    public Flux<Pull> findByProjectId(final Long projectId) {
        return Flux.empty();
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
                                 "                  head_user_github_id," +
                                 "                  base_last_commit_sha," +
                                 "                  base_branch," +
                                 "                  base_repo_id, " +
                                 "                  base_user_github_id," +
                                 "                  project_id," +
                                 "                  author_github_id) " +
                                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16) " +
                                 "ON CONFLICT (id) DO UPDATE " +
                                 "   SET title = $3," +
                                 "       open = $4," +
                                 "       updated_at = $6," +
                                 "       head_last_commit_sha = $7," +
                                 "       base_last_commit_sha = $11," +
                                 "       base_branch = $12");
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
                .bind("$10", pull.getHead().getUser().getId())
                .bind("$11", pull.getBase().getCommit().getSha())
                .bind("$12", pull.getBase().getBranch())
                .bind("$13", pull.getBase().getRepo().getId())
                .bind("$14", pull.getBase().getUser().getId())
                .bind("$15", pull.getProject().getId())
                .bind("$16", pull.getAuthor().getId());
    }

    private Pull convert(final Row row) {
        throw new RuntimeException();
    }
}
