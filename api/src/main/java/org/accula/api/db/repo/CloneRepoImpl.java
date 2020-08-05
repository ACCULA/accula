package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Clone;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;

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

        return manyWithConnection(connection -> {
            final var cloneList = clones instanceof ArrayList ? (ArrayList<Clone>) clones : new ArrayList<>(clones);

            final var statement = BatchStatement.of(connection, """ 
                    INSERT INTO clone (target_commit_sha, 
                                       target_repo_id, 
                                       target_file, 
                                       target_from_line, 
                                       target_to_line, 
                                       source_commit_sha, 
                                       source_repo_id, 
                                       source_file, 
                                       source_from_line, 
                                       source_to_line)  
                    VALUES ($collection) 
                    RETURNING id
                    """);
            statement.bind(cloneList, clone -> new Object[]{
                    clone.getTargetSnapshot().getSha(),
                    clone.getTargetSnapshot().getRepo().getId(),
                    clone.getTargetFile(),
                    clone.getTargetFromLine(),
                    clone.getTargetToLine(),
                    clone.getSourceSnapshot().getSha(),
                    clone.getSourceSnapshot().getRepo().getId(),
                    clone.getSourceFile(),
                    clone.getSourceFromLine(),
                    clone.getSourceToLine()
            });

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.columnFlux(result, "id", Long.class))
                    .zipWithIterable(cloneList, (id, clone) -> clone.toBuilder().id(id).build());
        });
    }

    @Override
    public Mono<Clone> findById(final Long id) {
        return Mono.error(new OperationNotSupportedException());
    }

    @Override
    public Flux<Clone> findByTargetCommitSnapshotSha(final String sha) {
        return manyWithConnection(connection -> Mono
                .from(connection
                        .createStatement("""
                                SELECT clone.id                    AS id,
                                       target.sha                  AS target_sha,
                                       target.branch               AS target_branch,
                                       target_snap_to_pull.pull_id AS target_pull_id,
                                       target_repo.id              AS target_repo_id,
                                       target_repo.name            AS target_repo_name,
                                       target_repo.description     AS target_repo_description,
                                       target_repo_owner.id        AS target_repo_owner_id,
                                       target_repo_owner.login     AS target_repo_owner_login,
                                       target_repo_owner.name      AS target_repo_owner_name,
                                       target_repo_owner.avatar    AS target_repo_owner_avatar,
                                       target_repo_owner.is_org    AS target_repo_owner_is_org,
                                       clone.target_file           AS target_file,
                                       clone.target_from_line      AS target_from_line,
                                       clone.target_to_line        AS target_to_line,
                                       source.sha                  AS source_sha,
                                       source.branch               AS source_branch,
                                       source_snap_to_pull.pull_id AS source_pull_id,
                                       source_repo.id              AS source_repo_id,
                                       source_repo.name            AS source_repo_name,
                                       source_repo.description     AS source_repo_description,
                                       source_repo_owner.id        AS source_repo_owner_id,
                                       source_repo_owner.login     AS source_repo_owner_login,
                                       source_repo_owner.name      AS source_repo_owner_name,
                                       source_repo_owner.avatar    AS source_repo_owner_avatar,
                                       source_repo_owner.is_org    AS source_repo_owner_is_org,
                                       clone.source_file           AS source_file,
                                       clone.source_from_line      AS source_from_line,
                                       clone.source_to_line        AS source_to_line
                                FROM clone
                                  JOIN commit_snapshot target
                                      ON clone.target_commit_sha = target.sha
                                          AND clone.target_repo_id = target.repo_id
                                  JOIN repo_github target_repo
                                      ON target.repo_id = target_repo.id
                                  JOIN user_github target_repo_owner
                                      ON target_repo.owner_id = target_repo_owner.id
                                  JOIN commit_snapshot_pull target_snap_to_pull
                                      ON target.sha = target_snap_to_pull.commit_snapshot_sha
                                          AND target.repo_id = target_snap_to_pull.commit_snapshot_repo_id
                                  JOIN commit_snapshot source
                                      ON clone.source_commit_sha = source.sha
                                          AND clone.source_repo_id = source.repo_id
                                  JOIN repo_github source_repo
                                      ON source.repo_id = source_repo.id
                                  JOIN user_github source_repo_owner
                                      ON source_repo.owner_id = source_repo_owner.id
                                  JOIN commit_snapshot_pull source_snap_to_pull
                                      ON source.sha = source_snap_to_pull.commit_snapshot_sha
                                          AND source.repo_id = source_snap_to_pull.commit_snapshot_repo_id
                                WHERE clone.target_commit_sha = $1
                                """)
                        .bind("$1", sha)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    @Override
    public Mono<Void> deleteByPullNumber(final long projectId, final int pullNumber) {
        return withConnection(connection -> ((PostgresqlStatement) connection
                .createStatement("""
                        DELETE FROM clone
                        WHERE id IN (SELECT clone.id
                                     FROM pull
                                        JOIN clone
                                            ON pull.head_commit_snapshot_sha = clone.target_commit_sha 
                                                AND pull.head_commit_snapshot_repo_id = clone.target_repo_id
                                     WHERE pull.project_id = $1 AND pull.number = $2)
                        """))
                .bind("$1", projectId)
                .bind("$2", pullNumber)
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .then());
    }

    private Clone convert(final Row row) {
        return Converters.convertClone(row,
                "id",
                "target_sha",
                "target_branch",
                "target_pull_id",
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
                "source_sha",
                "source_branch",
                "source_pull_id",
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
