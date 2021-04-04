package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Clone;
import org.accula.api.util.Lambda;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class CloneRepoImpl implements CloneRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<Clone> insert(final Collection<Clone> clones) {
        if (clones.isEmpty()) {
            return Flux.empty();
        }

        return transactionalMany(connection -> insertSnippets(connection, clones)
                .flatMapMany(Lambda.passingFirstArg(CloneRepoImpl::insertClones, connection)));
    }

    @Override
    public Mono<Clone> findById(final Long id) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Flux<Clone> findByPullNumber(final Long projectId, final Integer pullNumber) {
        return manyWithConnection(connection -> selectStatement(connection)
                .bind("$1", projectId)
                .bind("$2", pullNumber)
                .execute()
                .flatMap(result -> ConnectionProvidedRepo.convertMany(result, CloneRepoImpl::convert)));
    }

    @Override
    public Mono<Void> deleteByPullNumber(final Long projectId, final Integer pullNumber) {
        return transactional(connection -> deleteClonesByPullNumber(connection, projectId, pullNumber)
                .then(deleteNoLongerReferencedCloneSnippets(connection)));
    }

    private static Mono<List<Clone>> insertSnippets(final Connection connection, final Collection<Clone> clones) {
        final var snippets = clones
                .stream()
                .flatMap(clone -> Stream.of(clone.target(), clone.source()));

        final var snippetsStatement = BatchStatement.of(connection, """
                INSERT INTO clone_snippet (commit_sha, repo_id, branch, pull_id, file, from_line, to_line)
                VALUES ($collection)
                RETURNING id
                """);
        snippetsStatement.bind(snippets, snippet -> new Object[]{
                snippet.snapshot().sha(),
                snippet.snapshot().repo().id(),
                snippet.snapshot().branch(),
                Objects.requireNonNull(snippet.snapshot().pullInfo(), "Snapshot pullInfo MUST NOT be null").id(),
                snippet.file(),
                snippet.fromLine(),
                snippet.toLine()
        });

        return snippetsStatement
                .execute()
                .flatMap(result -> ConnectionProvidedRepo.columnFlux(result, "id", Long.class))
                .window(2)
                .flatMap(Flux::collectList)
                .zipWithIterable(clones, (targetAndSourceIds, clone) -> clone
                        .toBuilder()
                        .target(clone.target().withId(targetAndSourceIds.get(0)))
                        .source(clone.source().withId(targetAndSourceIds.get(1)))
                        .build())
                .collectList();
    }

    private static Flux<Clone> insertClones(final Connection connection, final List<Clone> clones) {
        final var statement = BatchStatement.of(connection, """
                INSERT INTO clone (target_id, source_id)
                VALUES ($collection)
                RETURNING id
                """);
        statement.bind(clones, clone -> new Object[]{
                clone.target().id(),
                clone.source().id()
        });
        return statement
                .execute()
                .flatMap(result -> ConnectionProvidedRepo.columnFlux(result, "id", Long.class))
                .zipWithIterable(clones, (id, clone) -> clone.withId(id));
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        @Language("SQL") final var sql = """
                SELECT clone.id                    AS id,
                       clone.target_id             AS target_snippet_id,
                       target_commit.sha           AS target_commit_sha,
                       target_commit.is_merge      AS target_commit_is_merge,
                       target_commit.author_name   AS target_commit_author_name,
                       target_commit.author_email  AS target_commit_author_email,
                       target_commit.date          AS target_commit_date,
                       target_snippet.branch       AS target_branch,
                       target_snap_to_pull.pull_id AS target_pull_id,
                       target_pull.number          AS target_pull_number,
                       target_repo.id              AS target_repo_id,
                       target_repo.name            AS target_repo_name,
                       target_repo.description     AS target_repo_description,
                       target_repo_owner.id        AS target_repo_owner_id,
                       target_repo_owner.login     AS target_repo_owner_login,
                       target_repo_owner.name      AS target_repo_owner_name,
                       target_repo_owner.avatar    AS target_repo_owner_avatar,
                       target_repo_owner.is_org    AS target_repo_owner_is_org,
                       target_snippet.file         AS target_file,
                       target_snippet.from_line    AS target_from_line,
                       target_snippet.to_line      AS target_to_line,
                       clone.source_id             AS source_snippet_id,
                       source_commit.sha           AS source_commit_sha,
                       source_commit.is_merge      AS source_commit_is_merge,
                       source_commit.author_name   AS source_commit_author_name,
                       source_commit.author_email  AS source_commit_author_email,
                       source_commit.date          AS source_commit_date,
                       source_snippet.branch       AS source_branch,
                       source_snap_to_pull.pull_id AS source_pull_id,
                       source_pull.number          AS source_pull_number,
                       source_repo.id              AS source_repo_id,
                       source_repo.name            AS source_repo_name,
                       source_repo.description     AS source_repo_description,
                       source_repo_owner.id        AS source_repo_owner_id,
                       source_repo_owner.login     AS source_repo_owner_login,
                       source_repo_owner.name      AS source_repo_owner_name,
                       source_repo_owner.avatar    AS source_repo_owner_avatar,
                       source_repo_owner.is_org    AS source_repo_owner_is_org,
                       source_snippet.file         AS source_file,
                       source_snippet.from_line    AS source_from_line,
                       source_snippet.to_line      AS source_to_line
                FROM clone
                  JOIN clone_snippet target_snippet
                      ON clone.target_id = target_snippet.id
                  JOIN snapshot_pull target_snap_to_pull
                      ON target_snippet.commit_sha = target_snap_to_pull.snapshot_sha
                          AND target_snippet.repo_id = target_snap_to_pull.snapshot_repo_id
                          AND target_snippet.branch = target_snap_to_pull.snapshot_branch
                          AND target_snippet.pull_id = target_snap_to_pull.pull_id
                  JOIN commit target_commit
                      ON target_snippet.commit_sha = target_commit.sha
                  JOIN repo_github target_repo
                      ON target_snap_to_pull.snapshot_repo_id = target_repo.id
                  JOIN user_github target_repo_owner
                      ON target_repo.owner_id = target_repo_owner.id
                  JOIN pull target_pull
                      ON target_snap_to_pull.pull_id = target_pull.id
                  JOIN project
                      ON target_repo.id = project.github_repo_id
                  JOIN clone_snippet source_snippet
                      ON clone.source_id = source_snippet.id
                  JOIN snapshot_pull source_snap_to_pull
                      ON source_snippet.commit_sha = source_snap_to_pull.snapshot_sha
                          AND source_snippet.repo_id = source_snap_to_pull.snapshot_repo_id
                          AND source_snippet.branch = source_snap_to_pull.snapshot_branch
                  JOIN commit source_commit
                      ON source_snippet.commit_sha = source_commit.sha
                  JOIN repo_github source_repo
                      ON source_snap_to_pull.snapshot_repo_id = source_repo.id
                  JOIN user_github source_repo_owner
                      ON source_repo.owner_id = source_repo_owner.id
                  JOIN pull source_pull
                      ON source_snap_to_pull.pull_id = source_pull.id
                WHERE project.id = $1 AND target_pull.number = $2
                """;
        return (PostgresqlStatement) connection.createStatement(sql);
    }

    private static Mono<Void> deleteClonesByPullNumber(final Connection connection, final Long projectId, final Integer pullNumber) {
        return ((PostgresqlStatement) connection
                .createStatement("""
                        DELETE
                        FROM clone
                        WHERE clone.id IN (SELECT clone.id
                                           FROM pull
                                                    JOIN clone_snippet target
                                                         ON pull.id = target.pull_id
                                                    JOIN clone
                                                         ON target.id = clone.target_id
                                                    JOIN project
                                                         ON pull.base_snapshot_repo_id = project.github_repo_id
                                           WHERE project.id = $1
                                             AND pull.number = $2)
                        """))
                .bind("$1", projectId)
                .bind("$2", pullNumber)
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();
    }

    private static Mono<Void> deleteNoLongerReferencedCloneSnippets(final Connection connection) {
        return ((PostgresqlStatement) connection
                .createStatement("""
                        DELETE
                        FROM clone_snippet snippet
                        WHERE NOT EXISTS(
                                SELECT
                                FROM clone
                                WHERE snippet.id = clone.source_id
                                   OR snippet.id = clone.target_id
                            )
                        """))
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then();
    }

    private static Clone convert(final Row row) {
        return Converters.convertClone(row,
                "id",
                "target_snippet_id",
                "target_commit_sha",
                "target_commit_is_merge",
                "target_commit_author_name",
                "target_commit_author_email",
                "target_commit_date",
                "target_branch",
                "target_pull_id",
                "target_pull_number",
                "target_repo_id",
                "target_repo_name",
                "target_repo_description",
                "target_repo_owner_id",
                "target_repo_owner_login",
                "target_repo_owner_name",
                "target_repo_owner_avatar",
                "target_repo_owner_is_org",
                "target_file",
                "target_from_line",
                "target_to_line",
                "source_snippet_id",
                "source_commit_sha",
                "source_commit_is_merge",
                "source_commit_author_name",
                "source_commit_author_email",
                "source_commit_date",
                "source_branch",
                "source_pull_id",
                "source_pull_number",
                "source_repo_id",
                "source_repo_name",
                "source_repo_description",
                "source_repo_owner_id",
                "source_repo_owner_login",
                "source_repo_owner_name",
                "source_repo_owner_avatar",
                "source_repo_owner_is_org",
                "source_file",
                "source_from_line",
                "source_to_line"
        );
    }
}
