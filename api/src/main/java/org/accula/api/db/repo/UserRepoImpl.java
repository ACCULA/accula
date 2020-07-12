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
import java.util.stream.Collectors;

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
                .doOnSuccess(user -> onUpserts
                        .forEach(onUpsert -> onUpsert.onUpsert(user.getId())));
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
    public Flux<User> findByGithubIds(Collection<Long> ids) {
        final var adminIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return manyWithConnection(connection -> Mono
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
                                WHERE u.github_id IN ($1)
                                """.replace("$1", adminIds)) // TODO: replace with bind, now it's failing
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convert(result, this::convert)));
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
