package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ProjectRepoImpl implements ProjectRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Mono<Boolean> notExists(final Long githubRepoId) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("""
                                SELECT NOT exists(SELECT 0 
                                                  FROM project 
                                                  WHERE github_repo_id = $1) AS not_exists
                                """)
                        .bind("$1", githubRepoId)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.column(result, "not_exists", Boolean.class)));
    }

    @Override
    public Mono<Project> upsert(final GithubRepo githubRepo, final User creator) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("""
                                WITH upserted_gh_repo AS (
                                      INSERT INTO repo_github (id, name, owner_id, description)
                                      VALUES ($1, $2, $3, $4)
                                      ON CONFLICT (id) DO UPDATE
                                          SET name = $2,
                                              owner_id = $3,
                                              description = $4
                                      RETURNING id
                                )
                                INSERT INTO project (github_repo_id, creator_id)
                                SELECT id, $5
                                FROM upserted_gh_repo
                                ON CONFLICT (github_repo_id) DO UPDATE
                                    SET creator_id = $5
                                RETURNING id
                                """)
                        .bind("$1", githubRepo.getId())
                        .bind("$2", githubRepo.getName())
                        .bind("$3", githubRepo.getOwner().getId())
                        .bind("$4", githubRepo.getDescription())
                        .bind("$5", creator.getId())
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo
                        .convert(result, row -> Project.builder()
                                .id(Converters.value(row, "id", Long.class))
                                .githubRepo(githubRepo)
                                .creator(creator)
                                .admins(Collections.emptyList())
                                .build()
                        )));
    }

    @Override
    public Mono<Project> findById(final Long id) {
        return withConnection(connection -> Mono
                .from(selectByIdStatement(connection)
                        .bind("$1", id)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public Mono<Long> idByRepoId(final Long repoId) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("""
                                SELECT id
                                FROM project
                                WHERE github_repo_id = $1
                                """)
                        .bind("$1", repoId)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.column(result, "id", Long.class)));
    }

    @Override
    public Flux<Project> getTop(final int count) {
        return manyWithConnection(connection -> Mono
                .from(selectTopStatement(connection)
                        .bind("$1", count)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    @Override
    public Mono<Boolean> delete(final Long id, final Long creatorId) {
        return withConnection(connection -> Mono
                .from(((PostgresqlStatement) connection
                        .createStatement("""
                                DELETE FROM project
                                WHERE id = $1 AND creator_id = $2
                                """))
                        .bind("$1", id)
                        .bind("$2", creatorId)
                        .execute())
                .flatMap(PostgresqlResult::getRowsUpdated)
                .map(Integer.valueOf(1)::equals));
    }

    @Override
    public Mono<Boolean> hasCreatorOrAdminWithId(final Long projectId, final Long userId) {
        return withConnection(connection -> Mono
                .from(((PostgresqlStatement) connection
                        .createStatement("""
                                SELECT exists(SELECT 1
                                              FROM project
                                              WHERE id = $1 AND creator_id = $2)
                                           OR
                                       exists(SELECT 1
                                              FROM project_admin
                                              WHERE project_id = $1 AND admin_id = $2)
                                           AS exists
                                """))
                        .bind("$1", projectId)
                        .bind("$2", userId)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.column(result, "exists", Boolean.class)));
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, """
                WHERE p.id = $1
                GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id, pulls.count
                """);
    }

    private static PostgresqlStatement selectTopStatement(final Connection connection) {
        return selectStatement(connection, """
                GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id, pulls.count
                LIMIT $1
                """);
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String terminatingCondition) {
        @Language("SQL") final var sql = """
                SELECT p.id                                AS project_id,
                       project_repo.id                     AS project_repo_id,
                       project_repo.name                   AS project_repo_name,
                       project_repo.description            AS project_repo_description,
                       project_repo_owner.id               AS project_repo_owner_id,
                       project_repo_owner.login            AS project_repo_owner_login,
                       project_repo_owner.name             AS project_repo_owner_name,
                       project_repo_owner.avatar           AS project_repo_owner_avatar,
                       project_repo_owner.is_org           AS project_repo_owner_is_org,
                       project_creator.id                  AS project_creator_id,
                       project_creator.github_access_token AS project_creator_github_access_token,
                       project_creator_github_user.id      AS project_creator_github_user_id,
                       project_creator_github_user.login   AS project_creator_github_user_login,
                       project_creator_github_user.name    AS project_creator_github_user_name,
                       project_creator_github_user.avatar  AS project_creator_github_user_avatar,
                       project_creator_github_user.is_org  AS project_creator_github_user_is_org,
                       pulls.count                         AS project_open_pull_count

                FROM project p
                         JOIN repo_github project_repo
                              ON p.github_repo_id = project_repo.id
                         JOIN user_github project_repo_owner
                              ON project_repo.owner_id = project_repo_owner.id
                         JOIN user_ project_creator
                              ON p.creator_id = project_creator.id
                         JOIN user_github project_creator_github_user
                              ON project_creator.github_id = project_creator_github_user.id
                         LEFT JOIN (SELECT count(*) FILTER ( WHERE open ),
                                           project_id
                               FROM pull
                               GROUP BY project_id) pulls
                              ON p.id = pulls.project_id
                """;
        return (PostgresqlStatement) connection.createStatement(String.format("%s %s", sql, terminatingCondition));
    }

    private Project convert(final Row row) {
        return Project.builder()
                .id(Converters.value(row, "project_id", Long.class))
                .githubRepo(Converters.convertRepo(row,
                        "project_repo_id",
                        "project_repo_name",
                        "project_repo_description",
                        "project_repo_owner_id",
                        "project_repo_owner_login",
                        "project_repo_owner_name",
                        "project_repo_owner_avatar",
                        "project_repo_owner_is_org"))
                .creator(Converters.convertUser(row,
                        "project_creator_id",
                        "project_creator_github_access_token",
                        "project_creator_github_user_id",
                        "project_creator_github_user_login",
                        "project_creator_github_user_name",
                        "project_creator_github_user_avatar",
                        "project_creator_github_user_is_org"))
                .openPullCount(Converters.integer(row, "project_open_pull_count"))
                .admins(Collections.emptyList()) // TODO: fetch admin list
                .build();
    }
}
