package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.CodeLanguage;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.accula.api.db.repo.ConnectionProvidedRepo.convertMany;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ProjectRepoImpl implements ProjectRepo, ConnectionProvidedRepo {
    private final Set<OnConfUpdate> onConfUpdates = ConcurrentHashMap.newKeySet();
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
                                      INSERT INTO repo_github (id, name, is_private, owner_id, description)
                                      VALUES ($1, $2, $3, $4, $5)
                                      ON CONFLICT (id) DO UPDATE
                                          SET name = $2,
                                              is_private = $3,
                                              owner_id = $4,
                                              description = $5
                                      RETURNING id
                                )
                                INSERT INTO project (github_repo_id, creator_id)
                                SELECT id, $6
                                FROM upserted_gh_repo
                                ON CONFLICT (github_repo_id) DO UPDATE
                                    SET creator_id = $6
                                RETURNING id, state
                                """)
                        .bind("$1", githubRepo.id())
                        .bind("$2", githubRepo.name())
                        .bind("$3", githubRepo.isPrivate())
                        .bind("$4", githubRepo.owner().id())
                        .bind("$5", githubRepo.description())
                        .bind("$6", creator.id())
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo
                        .convert(result, row -> Project.builder()
                                .id(Converters.value(row, "id", Long.class))
                                .state(Converters.value(row, "state", Project.State.class))
                                .githubRepo(githubRepo)
                                .creator(creator)
                                .build()
                        )));
    }

    @Override
    public Mono<Void> updateState(final Long id, final Project.State state) {
        return withConnection(connection -> Mono
                .from(((PostgresqlStatement) connection
                        .createStatement("""
                                UPDATE project
                                SET state = $1
                                WHERE id = $2
                                """))
                        .bind("$1", state)
                        .bind("$2", id)
                        .execute())
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then());
    }

    @Override
    public Mono<Project> findById(final Long id) {
        return transactional(connection -> selectByIdStatement(connection)
            .bind("$1", id)
            .execute()
            .next()
            .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert))
            .flatMap(project -> secondaryRepos(connection, id)
                .map(project::withSecondaryRepos)));
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
                .flatMapMany(result -> convertMany(result, this::convert)));
    }

    @Override
    public Mono<Boolean> delete(final Long id, final Long creatorId) {
        return withConnection(connection -> Mono
                .from(((PostgresqlStatement) connection
                        .createStatement("""
                                WITH deleted_repo AS (
                                    DELETE FROM project
                                    WHERE id = $1 AND creator_id = $2
                                    RETURNING github_repo_id
                                )
                                DELETE
                                FROM repo_github
                                WHERE id = (SELECT github_repo_id
                                            FROM deleted_repo)
                                  AND NOT exists(SELECT 0
                                                 FROM project_repo
                                                 WHERE repo_id = repo_github.id)
                                """))
                        .bind("$1", id)
                        .bind("$2", creatorId)
                        .execute())
                .flatMap(PostgresqlResult::getRowsUpdated)
                .map(Long.valueOf(1L)::equals));
    }

    @Override
    public Mono<Boolean> hasAdmin(final Long projectId, final Long userId) {
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

    @Override
    public Mono<Project.Conf> upsertConf(final Long id, final Project.Conf conf) {
        return transactional(connection -> upsertAdmins(connection, id, conf)
                .then(upsertExcludedSourceAuthors(connection, id, conf))
                .then(upsertConf(connection, id, conf))
                .thenReturn(conf))
                .doOnNext(c -> onConfUpdates
                        .forEach(onConfUpdate -> onConfUpdate.onConfUpdate(id)));
    }

    @Override
    public Mono<Project.Conf> confById(final Long id) {
        return withConnection(connection -> Mono.from(((PostgresqlStatement) connection.createStatement("""
                SELECT conf.clone_min_token_count                               AS clone_min_token_count,
                       conf.file_min_similarity_index                           AS file_min_similarity_index,
                       COALESCE(admins.ids, Array[]::BIGINT[])                  AS admin_ids,
                       conf.excluded_files                                      AS excluded_files,
                       conf.languages                                           AS languages,
                       COALESCE(excluded_source_authors.ids, Array[]::BIGINT[]) AS excluded_source_authors_ids
                FROM project_conf conf
                         LEFT JOIN (SELECT this.project_id,
                                           array_agg(this.admin_id) AS ids
                                    FROM project_admin this
                                    GROUP BY this.project_id) admins
                                   ON conf.project_id = admins.project_id
                         LEFT JOIN (SELECT this.project_id,
                                           array_agg(this.excluded_source_author_id) AS ids
                                    FROM project_excluded_source_author this
                                    GROUP BY this.project_id) excluded_source_authors
                                   ON conf.project_id = excluded_source_authors.project_id
                WHERE conf.project_id = $1
                """))
                .bind("$1", id)
                .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convertConf)));
    }

    @Override
    public Mono<List<CodeLanguage>> supportedLanguages() {
        return withConnection(connection -> Flux
            .from(connection.createStatement("""
                    SELECT unnest(enum_range(NULL::code_language_enum)) AS language
                    """)
                .execute())
            .flatMap(result -> convertMany(result, ProjectRepoImpl::convertDetectorLanguage))
            .collectList());
    }

    @Override
    public Mono<Boolean> projectDoesNotContainRepo(final Long projectId, final Long repoId) {
        return withConnection(connection -> Mono
            .from(((PostgresqlStatement) connection
                .createStatement("""
                    SELECT NOT exists(SELECT 1
                                      FROM project
                                      WHERE id = $1 AND github_repo_id = $2)
                               AND
                           NOT exists(SELECT 1
                                      FROM project_repo
                                      WHERE project_id = $1 AND repo_id = $2)
                               AS not_exists
                    """))
                .bind("$1", projectId)
                .bind("$2", repoId)
                .execute())
            .flatMap(result -> ConnectionProvidedRepo.column(result, "not_exists", Boolean.class)));
    }

    @Override
    public Mono<Void> attachRepos(final Long projectId, final Collection<Long> repoIds) {
        if (repoIds.isEmpty()) {
            return Mono.empty();
        }
        return withConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                INSERT INTO project_repo (project_id, repo_id)
                VALUES ($collection)
                ON CONFLICT (project_id, repo_id) DO NOTHING
                """);
            statement.bind(repoIds, repoId -> Bindings.of(
                projectId,
                repoId
            ));
            return statement
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();
        });
    }

    @Override
    public Mono<User> findOwnerOfProjectContainingRepo(final Long repoId) {
        return transactional(connection -> ((PostgresqlStatement) connection
            .createStatement("""
                SELECT u.id,
                       u.github_access_token,
                       ug.id                  AS github_id,
                       ug.login               AS github_login,
                       ug.name                AS github_name,
                       ug.avatar              AS github_avatar,
                       ug.is_org              AS github_org,
                       u.role
                FROM project
                    JOIN user_ u
                        ON project.creator_id = u.id
                    JOIN user_github ug
                        ON u.github_id = ug.id
                WHERE github_repo_id = $1
                """))
            .bind("$1", repoId)
            .execute()
            .flatMap(result -> ConnectionProvidedRepo.convert(result, UserRepoImpl::convert))
            .next()
            .switchIfEmpty(((PostgresqlStatement) connection
                .createStatement("""
                    SELECT u.id,
                           u.github_access_token,
                           ug.id                  AS github_id,
                           ug.login               AS github_login,
                           ug.name                AS github_name,
                           ug.avatar              AS github_avatar,
                           ug.is_org              AS github_org,
                           u.role
                    FROM project_repo
                        JOIN project
                            ON project_repo.project_id = project.id
                        JOIN user_ u
                            ON project.creator_id = u.id
                        JOIN user_github ug
                            ON u.github_id = ug.id
                    WHERE repo_id = $1
                        AND u.github_access_token IS NOT NULL
                    LIMIT 1
                    """))
                .bind("$1", repoId)
                .execute()
                .flatMap(result -> ConnectionProvidedRepo.convert(result, UserRepoImpl::convert))
                .next())
        );
    }

    @Override
    public void addOnConfUpdate(final OnConfUpdate onConfUpdate) {
        onConfUpdates.add(onConfUpdate);
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, """
                WHERE p.id = $1
                GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id,
                         pulls.count, admins.ids
                """);
    }

    private static PostgresqlStatement selectTopStatement(final Connection connection) {
        return selectStatement(connection, """
                GROUP BY p.id, project_repo.id, project_repo_owner.id, project_creator.id, project_creator_github_user.id,
                         pulls.count, admins.ids
                LIMIT $1
                """);
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String terminatingCondition) {
        @Language("SQL") final var sql = """
                SELECT p.id                                AS project_id,
                       p.state                             AS project_state,
                       project_repo.id                     AS project_repo_id,
                       project_repo.is_private             AS project_repo_is_private,
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
                       project_creator.role                AS project_creator_role,
                       pulls.count                         AS project_open_pull_count,
                       admins.ids                          AS project_admins

                FROM project p
                         JOIN repo_github project_repo
                              ON p.github_repo_id = project_repo.id
                         JOIN user_github project_repo_owner
                              ON project_repo.owner_id = project_repo_owner.id
                         JOIN user_ project_creator
                              ON p.creator_id = project_creator.id
                         JOIN user_github project_creator_github_user
                              ON project_creator.github_id = project_creator_github_user.id
                         LEFT JOIN (SELECT count(*) FILTER ( WHERE pull.open ),
                                           pull_proj.id AS project_id
                               FROM pull
                                 JOIN project pull_proj
                                   ON pull.base_snapshot_repo_id = pull_proj.github_repo_id
                               GROUP BY pull_proj.id) pulls
                              ON p.id = pulls.project_id
                         LEFT JOIN (SELECT this.project_id,
                                           array_agg(this.admin_id) AS ids
                                    FROM project_admin this
                                    GROUP BY this.project_id) admins
                                   ON p.id = admins.project_id
                """;
        return (PostgresqlStatement) connection.createStatement(String.format("%s %s", sql, terminatingCondition));
    }

    private static Mono<List<GithubRepo>> secondaryRepos(final Connection connection, final Long projectId) {
        return GithubRepoRepoImpl
            .selectStatement(connection,
                "JOIN project_repo ON repo.id = project_repo.repo_id",
                "WHERE project_repo.project_id = $1",
                "ORDER BY repo_owner_login, repo_name DESC"
            )
            .bind("$1", projectId)
            .execute()
            .flatMap(result -> convertMany(result, GithubRepoRepoImpl::convert))
            .collectList();
    }

    private Mono<Void> upsertAdmins(final Connection connection, final Long projectId, final Project.Conf conf) {
        final var deleteProjectAdmins = ((PostgresqlStatement) connection.createStatement("""
                DELETE FROM project_admin
                WHERE project_id = $1
                """))
                .bind("$1", projectId)
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();

        final Mono<Void> insertNewAdmins;
        if (conf.adminIds().isEmpty()) {
            insertNewAdmins = Mono.empty();
        } else {
            final var insertAdminsBackStatement = BatchStatement.of(connection, """
                    INSERT INTO project_admin (project_id, admin_id)
                    VALUES ($collection)
                    """);
            insertAdminsBackStatement.bind(conf.adminIds(), adminId -> Bindings.of(
                    projectId,
                    adminId
            ));
            insertNewAdmins = insertAdminsBackStatement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .then();
        }

        return deleteProjectAdmins.then(insertNewAdmins);
    }

    private Mono<Void> upsertExcludedSourceAuthors(final Connection connection,
                                                   final Long projectId,
                                                   final Project.Conf conf) {
        final var deleteSourceAuthors = ((PostgresqlStatement) connection.createStatement("""
                DELETE FROM project_excluded_source_author
                WHERE project_id = $1
                """))
            .bind("$1", projectId)
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();

        final Mono<Void> insertNewSourceAuthors;
        if (conf.excludedSourceAuthorIds().isEmpty()) {
            insertNewSourceAuthors = Mono.empty();
        } else {
            final var insertSourceAuthorsBackStatement = BatchStatement.of(connection, """
                    INSERT INTO project_excluded_source_author (project_id, excluded_source_author_id)
                    VALUES ($collection)
                    """);
            insertSourceAuthorsBackStatement.bind(conf.excludedSourceAuthorIds(), excludedSourceAuthorId -> Bindings.of(
                projectId,
                excludedSourceAuthorId
            ));
            insertNewSourceAuthors = insertSourceAuthorsBackStatement
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();
        }

        return deleteSourceAuthors.then(insertNewSourceAuthors);
    }

    private Mono<Void> upsertConf(final Connection connection, final Long projectId, final Project.Conf conf) {
        return ((PostgresqlStatement) connection.createStatement("""
                INSERT INTO project_conf (project_id, clone_min_token_count, file_min_similarity_index, excluded_files, languages)
                VALUES ($1, $2, $3, $4, $5)
                ON CONFLICT (project_id) DO UPDATE
                      SET clone_min_token_count = $2,
                          file_min_similarity_index = $3,
                          excluded_files = $4,
                          languages = $5
                """))
                .bind("$1", projectId)
                .bind("$2", conf.cloneMinTokenCount())
                .bind("$3", conf.fileMinSimilarityIndex())
                .bind("$4", conf.excludedFiles().toArray(new String[0]))
                .bind("$5", conf.languages().toArray(CodeLanguage[]::new))
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();
    }

    private Project convert(final Row row) {
        return Project.builder()
                .id(Converters.value(row, "project_id", Long.class))
                .state(Converters.value(row, "project_state", Project.State.class))
                .githubRepo(Converters.convertRepo(row,
                        "project_repo_id",
                        "project_repo_name",
                        "project_repo_is_private",
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
                        "project_creator_github_user_is_org",
                        "project_creator_role"))
                .openPullCount(Converters.integer(row, "project_open_pull_count", 0))
                .adminIds(Converters.ids(row, "project_admins"))
                .build();
    }

    private Project.Conf convertConf(final Row row) {
        return Project.Conf.builder()
                .adminIds(Converters.ids(row, "admin_ids"))
                .cloneMinTokenCount(Converters.integer(row, "clone_min_token_count"))
                .fileMinSimilarityIndex(Converters.integer(row, "file_min_similarity_index"))
                .excludedFiles(Converters.strings(row, "excluded_files"))
                .languages(List.of(Converters.value(row, "languages", CodeLanguage[].class)))
                .excludedSourceAuthorIds(Converters.ids(row, "excluded_source_authors_ids"))
                .build();
    }

    private static CodeLanguage convertDetectorLanguage(final Row row) {
        return Converters.value(row, "language", CodeLanguage.class);
    }
}
