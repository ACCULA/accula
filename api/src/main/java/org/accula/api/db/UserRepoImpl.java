package org.accula.api.db;

import io.r2dbc.pool.ConnectionPool;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public final class UserRepoImpl implements UserRepo {
    private final Map<Integer, OnUpsert> onUpserts = new ConcurrentHashMap<>();
    private final ConnectionPool connectionPool;

    @Override
    public Mono<Long> upsert(final Long ghId,
                             final String ghLogin,
                             final String ghName,
                             final String ghAvatar,
                             final String ghAccessToken) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("WITH upserted_gh_user AS (" +
                                                 "INSERT INTO user_github (id, login, name, avatar) " +
                                                 "VALUES ($1, $2, $3, $4)" +
                                                 "ON CONFLICT (id) DO UPDATE " +
                                                 "   SET login = $2, " +
                                                 "       name = $3, " +
                                                 "       avatar = $4 " +
                                                 "RETURNING id" +
                                                 ")" +
                                                 "INSERT INTO user_internal (gh_id, gh_access_token) " +
                                                 "SELECT id, $5 " +
                                                 "FROM upserted_gh_user " +
                                                 "ON CONFLICT (gh_id) DO UPDATE " +
                                                 "SET  gh_access_token = $5 " +
                                                 "RETURNING id")
                                //@formatter:on
                                .bind("$1", ghId)
                                .bind("$2", ghLogin)
                                .bind("$3", ghName)
                                .bind("$4", ghAvatar)
                                .bind("$5", ghAccessToken)
                                .execute())
                        .flatMap(result -> Mono.from(result
                                .map((row, metadata) -> row.get("id", Long.class))))
                        .flatMap(id -> Mono.from(connection.close()).thenReturn(id))
                        .doOnSuccess(id -> onUpserts
                                .forEach((k, onUpsert) -> onUpsert.onUpsert(id))));
    }

    @Override
    public Mono<User> get(Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT ui.id," +
                                                 "       ug.id AS gh_id," +
                                                 "       ug.login AS gh_login," +
                                                 "       ug.name AS gh_name," +
                                                 "       ug.avatar AS gh_avatar," +
                                                 "       ui.gh_access_token " +
                                                 "FROM user_internal ui " +
                                                 "  JOIN user_github ug ON ui.gh_id = ug.id " +
                                                 "WHERE ui.id = $1 " +
                                                 "LIMIT 1")
                                .bind("$1", id)
                                //@formatter:on
                                .execute())
                        .flatMap(result -> Mono.from(result
                                .map((row, metadata) -> User
                                        .builder()
                                        .id(Objects.requireNonNull(row.get("id", Long.class)))
                                        .ghId(Objects.requireNonNull(row.get("gh_id", Long.class)))
                                        .ghLogin(Objects.requireNonNull(row.get("gh_login", String.class)))
                                        .ghName(row.get("gh_name", String.class))
                                        .ghAvatar(Objects.requireNonNull(row.get("gh_avatar", String.class)))
                                        .ghAccessToken(Objects.requireNonNull(row.get("gh_access_token", String.class)))
                                        .build())))
                        .flatMap(user -> Mono.from(connection.close()).thenReturn(user)));
    }

    @Override
    public void addOnUpsert(final OnUpsert onUpsert) {
        onUpserts.put(onUpsert.hashCode(), onUpsert);
    }
}
