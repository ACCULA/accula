package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
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
public final class CloneRepoImpl implements CloneRepo {
    private final ConnectionPool connectionPool;

    @Override
    public Mono<Clone> insert(final Clone clone) {
        return Mono.error(new OperationNotSupportedException());
    }

    @Override
    public Flux<Clone> insert(final Collection<Clone> clones) {
        if (clones.isEmpty()) {
            return Flux.empty();
        }
        final var cloneList = clones instanceof ArrayList ? (ArrayList<Clone>) clones : new ArrayList<>((clones));

        return connectionPool
                .create()
                .flatMapMany(connection -> {
                    final var statement = insertStatement(connection);
                    cloneList.forEach(clone -> applyInsertBindings(clone, statement).add());
                    statement.fetchSize(cloneList.size());

                    return Repos.convertMany(statement.execute(), connection, row -> Converters
                            .value(row, "id", Long.class))
                            .zipWithIterable(cloneList, (id, clone) -> clone.toBuilder().id(id).build());
                });
    }

    @Override
    public Mono<Clone> findById(final Long id) {
        return Mono.error(new OperationNotSupportedException());
    }

    @Override
    public Flux<Clone> findByTargetCommitSnapshotSha(final String sha) {
        return connectionPool
                .create()
                .flatMapMany(connection -> Mono
                        .from(connection
                                //@formatter:off
                                .createStatement("SELECT clone.id                    AS id," +
                                                 "       target.sha                  AS target_sha," +
                                                 "       target.branch               AS target_branch," +
                                                 "       target_snap_to_pull.pull_id AS target_pull_id," +
                                                 "       target_repo.id              AS target_repo_id," +
                                                 "       target_repo.name            AS target_repo_name," +
                                                 "       target_repo.description     AS target_repo_description," +
                                                 "       target_repo_owner.id        AS target_repo_owner_id," +
                                                 "       target_repo_owner.login     AS target_repo_owner_login," +
                                                 "       target_repo_owner.name      AS target_repo_owner_name," +
                                                 "       target_repo_owner.avatar    AS target_repo_owner_avatar," +
                                                 "       target_repo_owner.is_org    AS target_repo_owner_is_org," +
                                                 "       clone.target_file           AS target_file," +
                                                 "       clone.target_from_line      AS target_from_line," +
                                                 "       clone.target_to_line        AS target_to_line," +
                                                 "       source.sha                  AS source_sha," +
                                                 "       source.branch               AS source_branch," +
                                                 "       source_snap_to_pull.pull_id AS source_pull_id," +
                                                 "       source_repo.id              AS source_repo_id," +
                                                 "       source_repo.name            AS source_repo_name," +
                                                 "       source_repo.description     AS source_repo_description," +
                                                 "       source_repo_owner.id        AS source_repo_owner_id," +
                                                 "       source_repo_owner.login     AS source_repo_owner_login," +
                                                 "       source_repo_owner.name      AS source_repo_owner_name," +
                                                 "       source_repo_owner.avatar    AS source_repo_owner_avatar," +
                                                 "       source_repo_owner.is_org    AS source_repo_owner_is_org," +
                                                 "       clone.source_file           AS source_file," +
                                                 "       clone.source_from_line      AS source_from_line," +
                                                 "       clone.source_to_line        AS source_to_line " +
                                                 "FROM clone " +
                                                 "  JOIN commit_snapshot target" +
                                                 "      ON clone.target_commit_sha = target.sha" +
                                                 "          AND clone.target_repo_id = target.repo_id" +
                                                 "  JOIN repo_github target_repo" +
                                                 "      ON target.repo_id = target_repo.id" +
                                                 "  JOIN user_github target_repo_owner" +
                                                 "      ON target_repo.owner_id = target_repo_owner.id" +
                                                 "  JOIN commit_snapshot_pull target_snap_to_pull" +
                                                 "      ON target.sha = target_snap_to_pull.commit_snapshot_sha" +
                                                 "          AND target.repo_id = target_snap_to_pull.commit_snapshot_repo_id" +
                                                 "  JOIN commit_snapshot source" +
                                                 "      ON clone.source_commit_sha = source.sha" +
                                                 "          AND clone.source_repo_id = source.repo_id" +
                                                 "  JOIN repo_github source_repo" +
                                                 "      ON source.repo_id = source_repo.id" +
                                                 "  JOIN user_github source_repo_owner" +
                                                 "      ON source_repo.owner_id = source_repo_owner.id " +
                                                 "  JOIN commit_snapshot_pull source_snap_to_pull" +
                                                 "      ON source.sha = source_snap_to_pull.commit_snapshot_sha" +
                                                 "          AND source.repo_id = source_snap_to_pull.commit_snapshot_repo_id " +
                                                 "WHERE clone.target_commit_sha = $1")
                                //@formatter:on
                                .bind("$1", sha)
                                .execute())
                        .flatMapMany(result -> Repos.convertMany(result, connection, this::convert)));
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        //@formatter:off
        return (PostgresqlStatement) connection
                .createStatement("INSERT INTO clone (" +
                                 "                   target_commit_sha," +
                                 "                   target_repo_id," +
                                 "                   target_file," +
                                 "                   target_from_line," +
                                 "                   target_to_line," +
                                 "                   source_commit_sha," +
                                 "                   source_repo_id," +
                                 "                   source_file," +
                                 "                   source_from_line," +
                                 "                   source_to_line) " +
                                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) " +
                                 "RETURNING id");
        //@formatter:on
    }

    private static PostgresqlStatement applyInsertBindings(final Clone clone, final PostgresqlStatement statement) {
        return statement
                .bind("$1", clone.getTargetSnapshot().getCommit().getSha())
                .bind("$2", clone.getTargetSnapshot().getRepo().getId())
                .bind("$3", clone.getTargetFile())
                .bind("$4", clone.getTargetFromLine())
                .bind("$5", clone.getTargetToLine())
                .bind("$6", clone.getSourceSnapshot().getCommit().getSha())
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
