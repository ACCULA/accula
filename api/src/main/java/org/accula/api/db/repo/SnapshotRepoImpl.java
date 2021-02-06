package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Snapshot;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class SnapshotRepoImpl implements SnapshotRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<Snapshot> insert(final Collection<Snapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO snapshot (sha, repo_id, branch)
                    VALUES ($collection)
                    ON CONFLICT (sha, repo_id) DO NOTHING
                    """);
            statement.bind(snapshots, commitSnapshot -> new Object[]{
                    commitSnapshot.sha(),
                    commitSnapshot.repo().id(),
                    commitSnapshot.branch()
            });

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(snapshots));
        });
    }

    @Override
    public Flux<Snapshot> mapToPulls(final Collection<Snapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return Flux.empty();
        }

        if (snapshots.stream().anyMatch(commitSnapshot -> commitSnapshot.pullId() == null)) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO snapshot_pull (snapshot_sha, snapshot_repo_id, pull_id)
                    VALUES ($collection)
                    ON CONFLICT (snapshot_sha, snapshot_repo_id, pull_id) DO NOTHING
                    """);
            statement.bind(snapshots, commitSnapshot -> new Object[]{
                    commitSnapshot.sha(),
                    commitSnapshot.repo().id(),
                    commitSnapshot.pullId()
            });

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(snapshots));
        });
    }

    @Override
    public Flux<Snapshot> findById(final Collection<Snapshot.Id> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }
        return manyWithConnection(connection -> {
            final var statement = selectStatement(connection);
            ids.forEach(id -> applySelectBindings(id, statement).add());

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert));
        });
    }

    private static PostgresqlStatement selectStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        SELECT snap.sha            AS snap_sha,
                               snap.branch         AS snap_branch,
                               repo.id             AS repo_id,
                               repo.name           AS repo_name,
                               repo.description    AS repo_description,
                               repo_owner.id       AS repo_owner_id,
                               repo_owner.login    AS repo_owner_login,
                               repo_owner.name     AS repo_owner_name,
                               repo_owner.avatar   AS repo_owner_avatar,
                               repo_owner.is_org   AS repo_owner_is_org
                        FROM snapshot snap
                           JOIN repo_github repo
                               ON snap.repo_id = repo.id
                           JOIN user_github repo_owner
                               ON repo.owner_id = repo_owner.id
                        WHERE snap.sha = $1 AND snap.repo_id = $2
                        """);
    }

    private static PostgresqlStatement applySelectBindings(final Snapshot.Id id, final PostgresqlStatement statement) {
        return statement
                .bind("$1", id.sha())
                .bind("$2", id.repoId());
    }

    private Snapshot convert(final Row row) {
        return Converters.convertCommitSnapshot(row,
                "snap_sha",
                "snap_branch",
                Converters.NOTHING,
                "repo_id",
                "repo_name",
                "repo_description",
                "repo_owner_id",
                "repo_owner_login",
                "repo_owner_name",
                "repo_owner_avatar",
                "repo_owner_is_org");
    }
}
