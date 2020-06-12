package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Row;
import lombok.Getter;
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
public final class UserRepoImpl implements UserRepo, ConnectionProvidedRepo {
    private final Set<OnUpsert> onUpserts = ConcurrentHashMap.newKeySet();
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Mono<User> upsert(final GithubUser githubUser, final String githubAccessToken) {
        return withConnection(connection -> Mono
                .from(applyInsertBindings(githubUser, githubAccessToken, (PostgresqlStatement) connection
                        .createStatement("WITH upserted_gh_user AS (\n" +
                                         "INSERT INTO user_github (id, login, name, avatar, is_org)\n" +
                                         "VALUES ($1, $2, $3, $4, $5)\n" +
                                         "ON CONFLICT (id) DO UPDATE\n" +
                                         "   SET login = $2,\n" +
                                         "       name = COALESCE($3, user_github.name),\n" +
                                         "       avatar = $4,\n" +
                                         "       is_org = $5\n" +
                                         "RETURNING id\n" +
                                         ")\n" +
                                         "INSERT INTO user_ (github_id, github_access_token)\n" +
                                         "SELECT id, $6\n" +
                                         "FROM upserted_gh_user\n" +
                                         "ON CONFLICT (github_id) DO UPDATE\n" +
                                         "  SET github_access_token = $6\n" +
                                         "RETURNING id\n"))
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo
                        .convert(result, row -> new User(
                                Converters.value(row, "id", Long.class),
                                githubAccessToken,
                                githubUser
                        ))))
                .doOnSuccess(user -> onUpserts
                        .forEach(onUpsert -> onUpsert.onUpsert(user.getId())));
    }

    @Override
    public Mono<User> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("SELECT u.id,\n" +
                                         "       ug.id                 AS github_id,\n" +
                                         "       ug.login              AS github_login,\n" +
                                         "       ug.name               AS github_name,\n" +
                                         "       ug.avatar             AS github_avatar,\n" +
                                         "       ug.is_org             AS github_org,\n" +
                                         "       u.github_access_token\n" +
                                         "FROM user_ u\n" +
                                         "  JOIN user_github ug ON u.github_id = ug.id\n" +
                                         "WHERE u.id = $1\n")
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public void addOnUpsert(final OnUpsert onUpsert) {
        onUpserts.add(onUpsert);
    }

    private static PostgresqlStatement applyInsertBindings(final GithubUser githubUser,
                                                           final String githubAccessToken,
                                                           final PostgresqlStatement statement) {
        return GithubUserRepoImpl
                .applyInsertBindings(githubUser, statement)
                .bind("$6", githubAccessToken);
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
