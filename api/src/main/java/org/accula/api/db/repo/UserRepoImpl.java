package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class UserRepoImpl implements UserRepo {
    private final Set<OnUpsert> onUpserts = ConcurrentHashMap.newKeySet();
    private final ConnectionPool connectionPool;

    @Override
    public Mono<User> upsert(final GithubUser githubUser, final String githubAccessToken) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("WITH upserted_gh_user AS (" +
                                                 "INSERT INTO user_github (id, login, name, avatar, is_org) " +
                                                 "VALUES ($1, $2, $3, $4, $5)" +
                                                 "ON CONFLICT (id) DO UPDATE " +
                                                 "   SET login = $2, " +
                                                 "       name = $3, " +
                                                 "       avatar = $4," +
                                                 "       is_org = $5 " +
                                                 "RETURNING id" +
                                                 ")" +
                                                 "INSERT INTO user_ (github_id, github_access_token) " +
                                                 "SELECT id, $6 " +
                                                 "FROM upserted_gh_user " +
                                                 "ON CONFLICT (github_id) DO UPDATE " +
                                                 "SET  github_access_token = $6 " +
                                                 "RETURNING id")
                                //@formatter:on
                                .bind("$1", githubUser.getId())
                                .bind("$2", githubUser.getLogin())
                                .bind("$3", githubUser.getName())
                                .bind("$4", githubUser.getAvatar())
                                .bind("$5", githubUser.isOrganization())
                                .bind("$6", githubAccessToken)
                                .execute())
                        .flatMap(result -> Repos
                                .convert(result, connection, row -> new User(
                                        Converters.value(row, "id", Long.class),
                                        githubAccessToken,
                                        githubUser
                                )))
                        .doOnSuccess(user -> onUpserts
                                .forEach(onUpsert -> onUpsert.onUpsert(user.getId()))));
    }

    @Override
    public Mono<User> findById(Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT u.id," +
                                                 "       ug.id                 AS github_id," +
                                                 "       ug.login              AS github_login," +
                                                 "       ug.name               AS github_name," +
                                                 "       ug.avatar             AS github_avatar," +
                                                 "       ug.is_org             AS github_org," +
                                                 "       u.github_access_token " +
                                                 "FROM user_ u " +
                                                 "  JOIN user_github ug ON u.github_id = ug.id " +
                                                 "WHERE u.id = $1")
                                .bind("$1", id)
                                //@formatter:on
                                .execute())
                        .flatMap(result -> Repos
                                .convert(result, connection, this::convert)));
    }

    @Override
    public void addOnUpsert(final OnUpsert onUpsert) {
        onUpserts.add(onUpsert);
    }

    private User convert(final Row row) {
        return Converters.convertUser(row,
                "id",
                "github_access_token",
                "github_id",
                "github_login",
                "github_name",
                "github_avatar",
                "github_org");
    }
}
