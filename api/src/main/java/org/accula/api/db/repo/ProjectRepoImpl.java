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
                        .createStatement("SELECT NOT exists(SELECT 0\n" +
                                         "                  FROM project\n" +
                                         "                  WHERE github_repo_id = $1) AS not_exists\n")
                        .bind("$1", githubRepoId)
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.column(result, "not_exists", Boolean.class)));
    }

    @Override
    public Mono<Project> upsert(final GithubRepo githubRepo, final User creator) {
        return withConnection(connection -> Mono
                .from(connection
                        .createStatement("WITH upserted_gh_repo AS (\n" +
                                         "      INSERT INTO repo_github (id, name, owner_id, description)\n" +
                                         "      VALUES ($1, $2, $3, $4)\n" +
                                         "      ON CONFLICT (id) DO UPDATE\n" +
                                         "          SET name = $2,\n" +
                                         "              owner_id = $3,\n" +
                                         "              description = $4\n" +
                                         "      RETURNING id\n" +
                                         ")\n" +
                                         "INSERT INTO project (github_repo_id, creator_id)\n" +
                                         "SELECT id, $5\n" +
                                         "FROM upserted_gh_repo\n" +
                                         "ON CONFLICT (github_repo_id) DO UPDATE\n" +
                                         "    SET creator_id = $5\n" +
                                         "RETURNING id\n")
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
                        .createStatement("SELECT id\n" +
                                         "FROM project\n" +
                                         "WHERE github_repo_id = $1\n")
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
                        .createStatement("DELETE FROM project\n" +
                                         "WHERE id = $1 AND creator_id = $2\n"))
                        .bind("$1", id)
                        .bind("$2", creatorId)
                        .execute())
                .flatMap(PostgresqlResult::getRowsUpdated)
                .map(Integer.valueOf(1)::equals));
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, "WHERE p.id = $1\n" +
                                           "GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id, pulls.count\n");
    }

    private static PostgresqlStatement selectTopStatement(final Connection connection) {
        return selectStatement(connection, "GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id, pulls.count\n" +
                                           "LIMIT $1\n");
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String terminatingCondition) {
        @Language("SQL") final var sql = "SELECT p.id                                AS project_id,\n" +
                                         "       project_repo.id                     AS project_repo_id,\n" +
                                         "       project_repo.name                   AS project_repo_name,\n" +
                                         "       project_repo.description            AS project_repo_description,\n" +
                                         "       project_repo_owner.id               AS project_repo_owner_id,\n" +
                                         "       project_repo_owner.login            AS project_repo_owner_login,\n" +
                                         "       project_repo_owner.name             AS project_repo_owner_name,\n" +
                                         "       project_repo_owner.avatar           AS project_repo_owner_avatar,\n" +
                                         "       project_repo_owner.is_org           AS project_repo_owner_is_org,\n" +
                                         "       project_creator.id                  AS project_creator_id,\n" +
                                         "       project_creator.github_access_token AS project_creator_github_access_token,\n" +
                                         "       project_creator_github_user.id      AS project_creator_github_user_id,\n" +
                                         "       project_creator_github_user.login   AS project_creator_github_user_login,\n" +
                                         "       project_creator_github_user.name    AS project_creator_github_user_name,\n" +
                                         "       project_creator_github_user.avatar  AS project_creator_github_user_avatar,\n" +
                                         "       project_creator_github_user.is_org  AS project_creator_github_user_is_org,\n" +
                                         "       pulls.count                         AS project_open_pull_count\n" +
                                         "\n" +
                                         "FROM project p\n" +
                                         "         JOIN repo_github project_repo\n" +
                                         "              ON p.github_repo_id = project_repo.id\n" +
                                         "         JOIN user_github project_repo_owner\n" +
                                         "              ON project_repo.owner_id = project_repo_owner.id\n" +
                                         "         JOIN user_ project_creator\n" +
                                         "              ON p.creator_id = project_creator.id\n" +
                                         "         JOIN user_github project_creator_github_user\n" +
                                         "              ON project_creator.github_id = project_creator_github_user.id\n" +
                                         "         LEFT JOIN (SELECT count(*) FILTER ( WHERE open ),\n" +
                                         "                           project_id\n" +
                                         "               FROM pull\n" +
                                         "               GROUP BY project_id) pulls\n" +
                                         "              ON p.id = pulls.project_id\n";
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
                .build();
    }
}
