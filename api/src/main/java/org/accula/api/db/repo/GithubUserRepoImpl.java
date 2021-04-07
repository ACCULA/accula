package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubUser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class GithubUserRepoImpl implements GithubUserRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvidedRepo.ConnectionProvider connectionProvider;

    @Override
    public Flux<GithubUser> upsert(final Collection<GithubUser> users) {
        if (users.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO user_github (id, login, name, avatar, is_org)
                    VALUES ($collection)
                    ON CONFLICT (id) DO UPDATE
                       SET login = excluded.login,
                           name = COALESCE(excluded.name, user_github.name),
                           avatar = excluded.avatar
                    """);
            statement.bind(users, user -> new Object[]{
                    user.id(),
                    user.login(),
                    user.name(),
                    user.avatar(),
                    user.isOrganization()
            });

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(users));
        });
    }

    @Override
    public Mono<GithubUser> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(selectStatement(connection)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, GithubUserRepoImpl::convert)));
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        SELECT *
                        FROM user_github
                        WHERE id = $1
                        """);
    }

    private static GithubUser convert(final Row row) {
        return Converters.convertUser(row,
            "id",
            "login",
            "name",
            "avatar",
            "is_org");
    }
}
