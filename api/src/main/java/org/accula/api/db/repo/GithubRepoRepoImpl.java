package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubRepo;
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
public final class GithubRepoRepoImpl implements GithubRepoRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<GithubRepo> upsert(final Collection<GithubRepo> repos) {
        if (repos.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO repo_github (id, name, owner_id, description)
                    VALUES ($collection)
                    ON CONFLICT (id) DO UPDATE
                       SET name = excluded.name,
                           owner_id = excluded.owner_id,
                           description = excluded.description
                    """);
            statement.bind(repos, repo -> Bindings.of(
                    repo.id(),
                    repo.name(),
                    repo.owner().id(),
                    repo.description()
            ));

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(repos));
        });
    }

    @Override
    public Mono<GithubRepo> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(selectStatement(connection, "WHERE repo.id = $1")
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, GithubRepoRepoImpl::convert)));
    }

    @Override
    public Mono<GithubRepo> findByName(final String owner, final String name) {
        return withConnection(connection ->
                selectStatement(connection, "WHERE usr.login = $1 AND repo.name = $2")
                        .bind("$1", owner)
                        .bind("$2", name)
                        .execute()
                        .next()
                        .flatMap(result -> ConnectionProvidedRepo.convert(result, GithubRepoRepoImpl::convert)));
    }

    static PostgresqlStatement selectStatement(final Connection connection, final String joinClause, final String whereClause) {
        @Language("SQL") final var sql = """
                SELECT repo.id          AS repo_id,
                       repo.name        AS repo_name,
                       repo.description AS repo_description,
                       usr.id           AS repo_owner_id,
                       usr.login        AS repo_owner_login,
                       usr.name         AS repo_owner_name,
                       usr.avatar       AS repo_owner_avatar,
                       usr.is_org       AS repo_owner_is_org
                FROM repo_github repo
                   JOIN user_github usr
                       ON repo.owner_id = usr.id
                """;
        return (PostgresqlStatement) connection.createStatement("%s %s %s".formatted(sql, joinClause, whereClause));
    }

    static GithubRepo convert(final Row row) {
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

    private static PostgresqlStatement selectStatement(final Connection connection, final String whereClause) {
        return selectStatement(connection, Converters.EMPTY_CLAUSE, whereClause);
    }
}
