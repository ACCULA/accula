package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
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
                        .from(connection
                                //@formatter:off
                                .createStatement("INSERT INTO user_github (id, login, name, avatar, is_org) " +
                                                 "VALUES ($1, $2, $3, $4, $5) " +
                                                 "ON CONFLICT (id) DO UPDATE " +
                                                 "   SET login = $2," +
                                                 "       name = $3," +
                                                 "       avatar = $4")
                                //@formatter:on
                                .bind("$1", user.getId())
                                .bind("$2", user.getLogin())
                                .bind("$3", user.getName())
                                .bind("$4", user.getAvatar())
                                .bind("$5", user.isOrganization())
                                .execute())
                        .flatMap(result -> Mono.from(result.getRowsUpdated()))
                        .filter(Integer.valueOf(1)::equals)
                        .flatMap(rowsUpdated -> Repos.closeAndReturn(connection, user)));
    }

    @Override
    public Flux<GithubUser> upsert(final Collection<GithubUser> users) {
        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    //@formatter:off
                    final var statement = connection
                            .createStatement("INSERT INTO user_github (id, login, name, avatar, is_org) " +
                                             "VALUES ($1, $2, $3, $4, $5) " +
                                             "ON CONFLICT (id) DO UPDATE " +
                                             "   SET login = $2," +
                                             "       name = $3," +
                                             "       avatar = $4");
                    //@formatter:on
                    users.forEach(user -> statement
                            .bind("$1", user.getId())
                            .bind("$2", user.getLogin())
                            .bind("$3", user.getName())
                            .bind("$4", user.getAvatar())
                            .bind("$5", user.isOrganization())
                            .add());
                    return Mono
                            .from(statement.execute())
                            .flatMap(result -> Mono.from(result.getRowsUpdated()))
                            .filter(Integer.valueOf(users.size())::equals)
                            .flatMapMany(rowsUpdated -> Repos.closeAndReturn(connection, users));
                });
    }

    @Override
    public Mono<GithubUser> findById(final Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT * " +
                                                 "FROM user_github " +
                                                 "WHERE id = $1")
                                //@formatter:on
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
}
