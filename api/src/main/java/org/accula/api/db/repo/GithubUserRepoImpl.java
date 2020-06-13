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
@SuppressWarnings("PMD.ConfusingTernary")
@RequiredArgsConstructor
public final class GithubUserRepoImpl implements GithubUserRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvidedRepo.ConnectionProvider connectionProvider;

    @Override
    public Mono<GithubUser> upsert(final GithubUser user) {
        return withConnection(connection -> applyInsertBindings(user, insertStatement(connection))
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then(Mono.just(user)));
    }

    @Override
    public Flux<GithubUser> upsert(final Collection<GithubUser> users) {
        if (users.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = insertStatement(connection);
            users.forEach(user -> applyInsertBindings(user, statement).add());

            return statement.execute()
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

    private static PostgresqlStatement insertStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        INSERT INTO user_github (id, login, name, avatar, is_org)
                        VALUES ($1, $2, $3, $4, $5)
                        ON CONFLICT (id) DO UPDATE
                           SET login = $2,
                               name = COALESCE($3, user_github.name),
                               avatar = $4
                        """);
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        SELECT *
                        FROM user_github
                        WHERE id = $1
                        """);
    }

    static PostgresqlStatement applyInsertBindings(final GithubUser user, final PostgresqlStatement statement) {
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
