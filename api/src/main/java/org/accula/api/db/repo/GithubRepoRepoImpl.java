package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubRepo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class GithubRepoRepoImpl implements GithubRepoRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Mono<GithubRepo> upsert(final GithubRepo repo) {
        return withConnection(connection -> Mono
                .from(insertStatement(connection)
                        .bind("$1", repo.getId())
                        .bind("$2", repo.getName())
                        .bind("$3", repo.getOwner().getId())
                        .bind("$4", repo.getDescription())
                        .execute())
                .flatMap(result -> Mono.from(result.getRowsUpdated()))
                .filter(Integer.valueOf(1)::equals)
                .map(rowsUpdated -> repo));
    }

    @Override
    public Flux<GithubRepo> upsert(final Collection<GithubRepo> repos) {
        if (repos.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = insertStatement(connection);
            repos.forEach(repo -> statement
                    .bind("$1", repo.getId())
                    .bind("$2", repo.getName())
                    .bind("$3", repo.getOwner().getId())
                    .bind("$4", repo.getDescription())
                    .add());
            statement.fetchSize(repos.size());

            return statement.execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(repos));
        });
    }

    @Override
    public Mono<GithubRepo> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(selectStatement(connection)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO repo_github (id, name, owner_id, description)\n" +
                                 "VALUES ($1, $2, $3, $4)\n" +
                                 "ON CONFLICT (id) DO UPDATE\n" +
                                 "   SET name = $2,\n" +
                                 "       owner_id = $3,\n" +
                                 "       description = $4\n");
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("SELECT repo.id          AS repo_id,\n" +
                                 "       repo.name        AS repo_name,\n" +
                                 "       repo.description AS repo_description,\n" +
                                 "       usr.id           AS repo_owner_id,\n" +
                                 "       usr.login        AS repo_owner_login,\n" +
                                 "       usr.name         AS repo_owner_name,\n" +
                                 "       usr.avatar       AS repo_owner_avatar,\n" +
                                 "       usr.is_org       AS repo_owner_is_org\n" +
                                 "FROM repo_github repo\n" +
                                 "   JOIN user_github usr\n" +
                                 "       ON repo.owner_id = usr.id\n" +
                                 "WHERE repo.id = $1\n");
    }

    private GithubRepo convert(final Row row) {
        return Converters.convertRepo(row,
                "repo_id",
                "repo_name",
                "repo_description",
                "repo_owner_id",
                "repo_owner_login",
                "repo_owner_name",
                "repo_owner_avatar",
                "repo_owner_is_org"
        );
    }
}
