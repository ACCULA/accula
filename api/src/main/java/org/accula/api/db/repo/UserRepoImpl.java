package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.User;
import org.accula.api.db.model.User.Role;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.accula.api.db.repo.ConnectionProvidedRepo.convertMany;
import static org.accula.api.db.repo.Converters.EMPTY_CLAUSE;
import static org.accula.api.db.repo.Converters.SPACE;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class UserRepoImpl implements UserRepo, ConnectionProvidedRepo {
    private final Set<OnUpsert> onUpserts = ConcurrentHashMap.newKeySet();
    @Getter
    private final ConnectionProvider connectionProvider;

    public Mono<User> upsert(final User user) {
        return withConnection(connection -> Mono
            .from(applyInsertBindings(user, (PostgresqlStatement) connection
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
                    INSERT INTO user_ (github_id, github_access_token, role)
                    SELECT id, $6, $7
                    FROM upserted_gh_user
                    ON CONFLICT (github_id) DO UPDATE
                      SET github_access_token = $6,
                          role = $7
                    RETURNING id
                    """))
                .execute())
            .flatMap(result -> ConnectionProvidedRepo
                .convert(result, row -> user.withId(Converters.longInteger(row, "id")))))
            .doOnNext(u -> OnUpsert.forEach(onUpserts, u.id()));
    }

    @Override
    public Mono<User> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(select(connection, EMPTY_CLAUSE, "WHERE u.id = $1", EMPTY_CLAUSE)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, UserRepoImpl::convert)));
    }

    @Override
    public Flux<User> findByGithubIds(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> select(connection, """
            JOIN unnest($1) WITH ORDINALITY AS arr(id, ord)
                ON ug.id = arr.id
            """, EMPTY_CLAUSE, "ORDER BY arr.ord")
            .bind("$1", ids.toArray(new Long[0]))
            .execute()
            .flatMap(result -> convertMany(result, UserRepoImpl::convert)));
    }

    @Override
    public Flux<User> findAll() {
        return manyWithConnection(UserRepoImpl::findAll);
    }

    @Override
    public Mono<List<User>> setAdminRole(final Collection<Long> adminIds) {
        return Mono.defer(() -> {
            final var adminIdSet = adminIds instanceof Set<Long> s ? s : new HashSet<>(adminIds);
            return transactional(connection -> findAll(connection)
                .collectList()
                .flatMap(allUsers -> {
                    if (allUsers.isEmpty()) {
                        return Mono.empty();
                    }

                    final var newAdminIds = allUsers
                        .stream()
                        .map(User::id)
                        .filter(adminIdSet::contains)
                        .toArray(Long[]::new);

                    final var updatedAllUsers = allUsers
                        .stream()
                        .map(user -> {
                            if (adminIdSet.contains(user.id()) && user.is(Role.USER)) {
                                return user.withRole(Role.ADMIN);
                            } else if (!adminIdSet.contains(user.id()) && user.is(Role.ADMIN)) {
                                return user.withRole(Role.USER);
                            }
                            return user;
                        })
                        .toList();

                    return setUserRole(connection)
                        .thenEmpty(setAdminRole(connection, newAdminIds))
                        .thenReturn(updatedAllUsers);
                }))
                .doOnNext(__ -> OnUpsert.forEach(onUpserts, null));
        });
    }

    @Override
    public void addOnUpsert(final OnUpsert onUpsert) {
        onUpserts.add(onUpsert);
    }

    private static PostgresqlStatement select(final Connection connection,
                                              final String joinClause,
                                              final String whereClause,
                                              final String orderByClause) {
        @Language("SQL")
        final var sql = """
            SELECT u.id                  AS id,
                   ug.id                 AS github_id,
                   ug.login              AS github_login,
                   ug.name               AS github_name,
                   ug.avatar             AS github_avatar,
                   ug.is_org             AS github_org,
                   u.github_access_token AS github_access_token,
                   u.role                AS role
            FROM user_ u
                     JOIN user_github ug
                          ON u.github_id = ug.id
            """;
        final var sqlSb = new StringBuilder();
        sqlSb.append(sql);
        if (!joinClause.isBlank()) {
            sqlSb.append(SPACE);
            sqlSb.append(joinClause);
        }
        if (!whereClause.isBlank()) {
            sqlSb.append(SPACE);
            sqlSb.append(whereClause);
        }
        if (!orderByClause.isBlank()) {
            sqlSb.append(SPACE);
            sqlSb.append(orderByClause);
        }
        return (PostgresqlStatement) connection.createStatement(sqlSb.toString());
    }

    private static PostgresqlStatement applyInsertBindings(final User user,
                                                           final PostgresqlStatement statement) {
        final var githubUser = user.githubUser();
        statement.bind("$1", githubUser.id());
        statement.bind("$2", githubUser.login());
        if (githubUser.name() != null && !githubUser.name().isBlank()) {
            statement.bind("$3", githubUser.name());
        } else {
            statement.bindNull("$3", String.class);
        }
        statement.bind("$4", githubUser.avatar());
        statement.bind("$5", githubUser.isOrganization());
        statement.bind("$6", user.githubAccessToken());
        statement.bind("$7", user.role());

        return statement;
    }

    static User convert(final Row row) {
        return Converters.convertUser(row,
                "id",
                "github_access_token",
                "github_id",
                "github_login",
                "github_name",
                "github_avatar",
                "github_org",
                "role");
    }

    private static Flux<User> findAll(final Connection connection) {
        return select(connection, EMPTY_CLAUSE, EMPTY_CLAUSE, EMPTY_CLAUSE)
            .execute()
            .flatMap(result -> convertMany(result, UserRepoImpl::convert));
    }

    private static Mono<Void> setUserRole(final Connection connection) {
        return ((PostgresqlStatement) connection.createStatement("""
            UPDATE user_
            SET role = 'USER'
            WHERE role != 'ROOT'
            """))
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

    private static Mono<Void> setAdminRole(final Connection connection, final Long[] adminIds) {
        return ((PostgresqlStatement) connection.createStatement("""
            UPDATE user_
            SET role = 'ADMIN'
            WHERE role != 'ROOT' AND id = ANY($1)
            """))
            .bind("$1", adminIds)
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }
}
