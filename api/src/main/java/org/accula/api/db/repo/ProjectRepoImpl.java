package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.OperationNotSupportedException;
import java.util.Objects;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ProjectRepoImpl implements ProjectRepo {
    private final ConnectionPool connectionPool;

    @Override
    public Mono<Boolean> notExists(final Long githubRepoId) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono.from(connection
                        .createStatement("SELECT NOT exists(SELECT 0 FROM project WHERE github_repo_id = $1) AS not_exists")
                        .bind("$1", githubRepoId)
                        .execute())
                        .flatMap(result -> Repos.column(result, "not_exists", Boolean.class, connection)));
    }

    @Override
    public Mono<Project> upsert(final GithubRepo githubRepo, final User creator) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("WITH upserted_gh_repo AS (" +
                                                 "      INSERT INTO repo_github (id, name, owner_id, description) " +
                                                 "      VALUES ($1, $2, $3, $4) " +
                                                 "      ON CONFLICT (id) DO UPDATE " +
                                                 "          SET name = $2," +
                                                 "              owner_id = $3," +
                                                 "              description = $4 " +
                                                 "      RETURNING id" +
                                                 ")" +
                                                 "INSERT INTO project (github_repo_id, creator_id) " +
                                                 "SELECT id, $5 " +
                                                 "FROM upserted_gh_repo " +
                                                 "ON CONFLICT (github_repo_id) DO UPDATE " +
                                                 "    SET creator_id = $5 " +
                                                 "RETURNING id")
                                //@formatter:on
                                .bind("$1", githubRepo.getId())
                                .bind("$2", githubRepo.getName())
                                .bind("$3", githubRepo.getOwner().getId())
                                .bind("$4", githubRepo.getDescription())
                                .bind("$5", creator.getId())
                                .execute())
                        .flatMap(result -> Repos.convert(result, connection, row -> new Project(
                                Objects.requireNonNull(row.get("id", Long.class)),
                                githubRepo,
                                creator,
                                new User[0]
                        ))));
    }

    @Override
    public Mono<Project> get(final Long id) {
        return connectionPool
                .create()
                .flatMap(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT p.id                                AS project_id," +
                                                 "       project_repo.id                     AS project_repo_id," +
                                                 "       project_repo.name                   AS project_repo_name," +
                                                 "       project_repo.description            AS project_repo_description," +
                                                 "       project_repo_owner.id               AS project_repo_owner_id," +
                                                 "       project_repo_owner.login            AS project_repo_owner_login," +
                                                 "       project_repo_owner.name             AS project_repo_owner_name," +
                                                 "       project_repo_owner.avatar           AS project_repo_owner_avatar," +
                                                 "       project_repo_owner.is_org           AS project_repo_owner_is_org," +
                                                 "       project_creator.id                  AS project_creator_id," +
                                                 "       project_creator.github_access_token AS project_creator_github_access_token," +
                                                 "       project_creator_github_user.id      AS project_creator_github_user_id," +
                                                 "       project_creator_github_user.login   AS project_creator_github_user_login," +
                                                 "       project_creator_github_user.name    AS project_creator_github_user_name," +
                                                 "       project_creator_github_user.avatar  AS project_creator_github_user_avatar," +
                                                 "       project_creator_github_user.is_org  AS project_creator_github_user_is_org " +

                                                 "FROM project p " +
                                                 "         JOIN repo_github project_repo " +
                                                 "              ON p.github_repo_id = project_repo.id " +
                                                 "         JOIN user_github project_repo_owner " +
                                                 "              ON project_repo.owner_id = project_repo_owner.id " +
                                                 "         JOIN user_ project_creator " +
                                                 "              ON p.creator_id = project_creator.id " +
                                                 "         JOIN user_github project_creator_github_user " +
                                                 "              ON project_creator.github_id = project_creator_github_user.id " +
                                                 "WHERE p.id = $1")
                                //@formatter:on
                                .bind("$1", id)
                                .execute())
                        .flatMap(result -> Repos.convert(result, connection, this::convert)));
    }

    @Override
    public Flux<Project> getTop(final int count) {
        return connectionPool
                .create()
                .flatMapMany(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT p.id                                AS project_id," +
                                        "       project_repo.id                     AS project_repo_id," +
                                        "       project_repo.name                   AS project_repo_name," +
                                        "       project_repo.description            AS project_repo_description," +
                                        "       project_repo_owner.id               AS project_repo_owner_id," +
                                        "       project_repo_owner.login            AS project_repo_owner_login," +
                                        "       project_repo_owner.name             AS project_repo_owner_name," +
                                        "       project_repo_owner.avatar           AS project_repo_owner_avatar," +
                                        "       project_repo_owner.is_org           AS project_repo_owner_is_org," +
                                        "       project_creator.id                  AS project_creator_id," +
                                        "       project_creator.github_access_token AS project_creator_github_access_token," +
                                        "       project_creator_github_user.id      AS project_creator_github_user_id," +
                                        "       project_creator_github_user.login   AS project_creator_github_user_login," +
                                        "       project_creator_github_user.name    AS project_creator_github_user_name," +
                                        "       project_creator_github_user.avatar  AS project_creator_github_user_avatar," +
                                        "       project_creator_github_user.is_org  AS project_creator_github_user_is_org " +

                                        "FROM project p " +
                                        "         JOIN repo_github project_repo " +
                                        "              ON p.github_repo_id = project_repo.id " +
                                        "         JOIN user_github project_repo_owner " +
                                        "              ON project_repo.owner_id = project_repo_owner.id " +
                                        "         JOIN user_ project_creator " +
                                        "              ON p.creator_id = project_creator.id " +
                                        "         JOIN user_github project_creator_github_user " +
                                        "              ON project_creator.github_id = project_creator_github_user.id " +
                                        "LIMIT $1")
                                //@formatter:on
                                .bind("$1", count)
                                .execute())
                        .flatMapMany(result -> Repos.convertMany(result, connection, this::convert)));
    }

    @Override
    public Mono<Boolean> delete(final Long ghRepoId) {
        return Mono.error(new OperationNotSupportedException("DELETE /projects/{id} is not yet implemented"));
    }

    private Project convert(final Row row) {
        return new Project(
                Objects.requireNonNull(row.get("project_id", Long.class)),
                new GithubRepo(
                        Objects.requireNonNull(row.get("project_repo_id", Long.class)),
                        Objects.requireNonNull(row.get("project_repo_name", String.class)),
                        new GithubUser(
                                Objects.requireNonNull(row.get("project_repo_owner_id", Long.class)),
                                Objects.requireNonNull(row.get("project_repo_owner_login", String.class)),
                                Objects.requireNonNull(row.get("project_repo_owner_name", String.class)),
                                Objects.requireNonNull(row.get("project_repo_owner_avatar", String.class)),
                                Objects.requireNonNull(row.get("project_repo_owner_is_org", Boolean.class))
                        ),
                        Objects.requireNonNull(row.get("project_repo_description", String.class))
                ),
                new User(
                        Objects.requireNonNull(row.get("project_creator_id", Long.class)),
                        new GithubUser(
                                Objects.requireNonNull(row.get("project_creator_github_user_id", Long.class)),
                                Objects.requireNonNull(row.get("project_creator_github_user_login", String.class)),
                                Objects.requireNonNull(row.get("project_creator_github_user_name", String.class)),
                                Objects.requireNonNull(row.get("project_creator_github_user_avatar", String.class)),
                                Objects.requireNonNull(row.get("project_creator_github_user_is_org", Boolean.class))
                        ),
                        Objects.requireNonNull(row.get("project_creator_github_access_token", String.class))
                ),
                new User[0]
        );
    }
}
