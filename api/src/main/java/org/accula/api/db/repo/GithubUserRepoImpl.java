package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubUser;
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
                    user.getId(),
                    user.getLogin(),
                    user.getName(),
                    user.getAvatar(),
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
                .flatMap(result -> ConnectionProvidedRepo.convert(result, row -> new GithubUser(
                        Objects.requireNonNull(row.get("id", Long.class)),
                        Objects.requireNonNull(row.get("login", String.class)),
                        Objects.requireNonNull(row.get("name", String.class)),
                        Objects.requireNonNull(row.get("avatar", String.class)),
                        Objects.requireNonNull(row.get("is_org", Boolean.class))
                ))));
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        SELECT *
                        FROM user_github
                        WHERE id = $1
                        """);
    }
}
