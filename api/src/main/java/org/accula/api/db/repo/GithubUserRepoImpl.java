package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
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
public final class GithubUserRepoImpl implements GithubUserRepo {
    private final ConnectionPool connectionPool;

    @Override
    public Mono<GithubUser> upsert(final GithubUser user) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(applyInsertBindings(user, insertStatement(connection))
                                .execute())
                        .flatMap(result -> Mono.from(result.getRowsUpdated()))
                        .filter(Integer.valueOf(1)::equals)
                        .flatMap(rowsUpdated -> Repos.closeAndReturn(connection, user)));
    }

    @Override
    public Flux<GithubUser> upsert(final Collection<GithubUser> users) {
        if (users.isEmpty()) {
            return Flux.empty();
        }

        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = insertStatement(connection);
                    users.forEach(user -> applyInsertBindings(user, statement).add());
                    statement.fetchSize(users.size());

                    return statement.execute()
                            .flatMap(PostgresqlResult::getRowsUpdated)
                            .thenMany(Repos.closeAndReturn(connection, users));
                });
    }

    @Override
    public Mono<GithubUser> findById(final Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(selectStatement(connection)
                                .bind("$1", id)
                                .execute())
                        .flatMap(result -> Repos
                                .convert(result, connection, row -> new GithubUser(
                                        Objects.requireNonNull(row.get("id", Long.class)),
                                        Objects.requireNonNull(row.get("login", String.class)),
                                        Objects.requireNonNull(row.get("name", String.class)),
                                        Objects.requireNonNull(row.get("avatar", String.class)),
                                        Objects.requireNonNull(row.get("is_org", Boolean.class))
                                ))));
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO user_github (id, login, name, avatar, is_org) " +
                                 "VALUES ($1, $2, $3, $4, $5) " +
                                 "ON CONFLICT (id) DO UPDATE " +
                                 "   SET login = $2," +
                                 "       name = COALESCE($3, user_github.name)," +
                                 "       avatar = $4");
        //@formatter:on
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("SELECT * " +
                                 "FROM user_github " +
                                 "WHERE id = $1");
        //@formatter:on
    }

    static Statement applyInsertBindings(final GithubUser user, final Statement statement) {
        statement.bind("$1", user.getId());
        statement.bind("$2", user.getLogin());
        if (user.getName() != null && !user.getName().isBlank()) {
            statement.bind("$3", user.getName());
        } else {
            statement.bindNull("$3", String.class);
        }
        statement.bind("$4", user.getAvatar());
        statement.bind("$5", user.isOrganization());
        return statement;
    }
}
