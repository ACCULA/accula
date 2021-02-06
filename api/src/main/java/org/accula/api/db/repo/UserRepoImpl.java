package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
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
                        .createStatement("""
                                WITH upserted_gh_user AS (
                                INSERT INTO user_github (id, login, name, avatar, is_org)
                                VALUES ($1, $2, $3, $4, $5)
                                ON CONFLICT (id) DO UPDATE
                                   SET login = $2,
                                       name = COALESCE($3, user_github.name),
                                       avatar = $4,
                                       is_org = $5
                                RETURNING id
                                )
                                INSERT INTO user_ (github_id, github_access_token)
                                SELECT id, $6
                                FROM upserted_gh_user
                                ON CONFLICT (github_id) DO UPDATE
                                  SET github_access_token = $6
                                RETURNING id
                                """))
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo
                        .convert(result, row -> new User(
                                Converters.value(row, "id", Long.class),
                                githubAccessToken,
                                githubUser
                        ))))
                .doOnNext(user -> onUpserts
                        .forEach(onUpsert -> onUpsert.onUpsert(user.id())));
    }

    @Override
    public Mono<User> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("""
                                SELECT u.id,
                                       ug.id                 AS github_id,
                                       ug.login              AS github_login,
                                       ug.name               AS github_name,
                                       ug.avatar             AS github_avatar,
                                       ug.is_org             AS github_org,
                                       u.github_access_token
                                FROM user_ u
                                  JOIN user_github ug ON u.github_id = ug.id
                                WHERE u.id = $1
                                """)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public Flux<User> findByGithubIds(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = (PostgresqlStatement) connection.createStatement("""
                    SELECT u.id,
                           ug.id     AS github_id,
                           ug.login  AS github_login,
                           ug.name   AS github_name,
                           ug.avatar AS github_avatar,
                           ug.is_org AS github_org,
                           u.github_access_token
                    FROM user_ u
                             JOIN user_github ug
                                  ON u.github_id = ug.id
                             JOIN unnest($1) WITH ORDINALITY AS arr(id, ord)
                                  ON ug.id = arr.id
                    ORDER BY arr.ord
                    """);
            statement.bind("$1", ids.toArray(new Long[0]));

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.convertMany(result, this::convert));
        });
    }

    @Override
    public void addOnUpsert(final OnUpsert onUpsert) {
        onUpserts.add(onUpsert);
    }

    private static PostgresqlStatement applyInsertBindings(final GithubUser githubUser,
                                                           final String githubAccessToken,
                                                           final PostgresqlStatement statement) {
        statement.bind("$1", githubUser.id());
        statement.bind("$2", githubUser.login());
        if (githubUser.name() != null && !githubUser.name().isBlank()) {
            statement.bind("$3", githubUser.name());
        } else {
            statement.bindNull("$3", String.class);
        }
        statement.bind("$4", githubUser.avatar());
        statement.bind("$5", githubUser.isOrganization());
        statement.bind("$6", githubAccessToken);
        return statement;
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
