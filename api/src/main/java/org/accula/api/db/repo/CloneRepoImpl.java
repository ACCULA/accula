package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
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
    public Mono<Clone> insert(final Clone clone) {
        return Mono.error(new OperationNotSupportedException());
    }

    @Override
    public Flux<Clone> insert(final Collection<Clone> clones) {
        if (clones.isEmpty()) {
            return Flux.empty();
        }

        final var cloneList = clones instanceof ArrayList ? (ArrayList<Clone>) clones : new ArrayList<>(clones);

        return manyWithConnection(connection -> {
            final var statement = insertStatement(connection);
            cloneList.forEach(clone -> applyInsertBindings(clone, statement).add());
//            statement.fetchSize(cloneList.size());

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.column(result, "id", Long.class))
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
                        .createStatement("SELECT clone.id                    AS id,\n" +
                                         "       target.sha                  AS target_sha,\n" +
                                         "       target.branch               AS target_branch,\n" +
                                         "       target_snap_to_pull.pull_id AS target_pull_id,\n" +
                                         "       target_repo.id              AS target_repo_id,\n" +
                                         "       target_repo.name            AS target_repo_name,\n" +
                                         "       target_repo.description     AS target_repo_description,\n" +
                                         "       target_repo_owner.id        AS target_repo_owner_id,\n" +
                                         "       target_repo_owner.login     AS target_repo_owner_login,\n" +
                                         "       target_repo_owner.name      AS target_repo_owner_name,\n" +
                                         "       target_repo_owner.avatar    AS target_repo_owner_avatar,\n" +
                                         "       target_repo_owner.is_org    AS target_repo_owner_is_org,\n" +
                                         "       clone.target_file           AS target_file,\n" +
                                         "       clone.target_from_line      AS target_from_line,\n" +
                                         "       clone.target_to_line        AS target_to_line,\n" +
                                         "       source.sha                  AS source_sha,\n" +
                                         "       source.branch               AS source_branch,\n" +
                                         "       source_snap_to_pull.pull_id AS source_pull_id,\n" +
                                         "       source_repo.id              AS source_repo_id,\n" +
                                         "       source_repo.name            AS source_repo_name,\n" +
                                         "       source_repo.description     AS source_repo_description,\n" +
                                         "       source_repo_owner.id        AS source_repo_owner_id,\n" +
                                         "       source_repo_owner.login     AS source_repo_owner_login,\n" +
                                         "       source_repo_owner.name      AS source_repo_owner_name,\n" +
                                         "       source_repo_owner.avatar    AS source_repo_owner_avatar,\n" +
                                         "       source_repo_owner.is_org    AS source_repo_owner_is_org,\n" +
                                         "       clone.source_file           AS source_file,\n" +
                                         "       clone.source_from_line      AS source_from_line,\n" +
                                         "       clone.source_to_line        AS source_to_line\n" +
                                         "FROM clone\n" +
                                         "  JOIN commit_snapshot target\n" +
                                         "      ON clone.target_commit_sha = target.sha\n" +
                                         "          AND clone.target_repo_id = target.repo_id\n" +
                                         "  JOIN repo_github target_repo\n" +
                                         "      ON target.repo_id = target_repo.id\n" +
                                         "  JOIN user_github target_repo_owner\n" +
                                         "      ON target_repo.owner_id = target_repo_owner.id\n" +
                                         "  JOIN commit_snapshot_pull target_snap_to_pull\n" +
                                         "      ON target.sha = target_snap_to_pull.commit_snapshot_sha\n" +
                                         "          AND target.repo_id = target_snap_to_pull.commit_snapshot_repo_id\n" +
                                         "  JOIN commit_snapshot source\n" +
                                         "      ON clone.source_commit_sha = source.sha\n" +
                                         "          AND clone.source_repo_id = source.repo_id\n" +
                                         "  JOIN repo_github source_repo\n" +
                                         "      ON source.repo_id = source_repo.id\n" +
                                         "  JOIN user_github source_repo_owner\n" +
                                         "      ON source_repo.owner_id = source_repo_owner.id\n" +
                                         "  JOIN commit_snapshot_pull source_snap_to_pull\n" +
                                         "      ON source.sha = source_snap_to_pull.commit_snapshot_sha\n" +
                                         "          AND source.repo_id = source_snap_to_pull.commit_snapshot_repo_id\n" +
                                         "WHERE clone.target_commit_sha = $1\n")
                        .bind("$1", sha)
                        .execute())
                .flatMapMany(result -> ConnectionProvidedRepo.convertMany(result, this::convert)));
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO clone (target_commit_sha,\n" +
                                 "                   target_repo_id,\n" +
                                 "                   target_file,\n" +
                                 "                   target_from_line,\n" +
                                 "                   target_to_line,\n" +
                                 "                   source_commit_sha,\n" +
                                 "                   source_repo_id,\n" +
                                 "                   source_file,\n" +
                                 "                   source_from_line,\n" +
                                 "                   source_to_line)\n" +
                                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)\n" +
                                 "RETURNING id\n");
    }

    private static PostgresqlStatement applyInsertBindings(final Clone clone, final PostgresqlStatement statement) {
        return statement
                .bind("$1", clone.getTargetSnapshot().getSha())
                .bind("$2", clone.getTargetSnapshot().getRepo().getId())
                .bind("$3", clone.getTargetFile())
                .bind("$4", clone.getTargetFromLine())
                .bind("$5", clone.getTargetToLine())
                .bind("$6", clone.getSourceSnapshot().getSha())
                .bind("$7", clone.getSourceSnapshot().getRepo().getId())
                .bind("$8", clone.getSourceFile())
                .bind("$9", clone.getSourceFromLine())
                .bind("$10", clone.getSourceToLine());
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
