package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.CommitSnapshot;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class CommitSnapshotRepoImpl implements CommitSnapshotRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Mono<CommitSnapshot> insert(final CommitSnapshot commitSnapshot) {
        return withConnection(connection -> Mono
                .from(applyInsertBindings(commitSnapshot, insertStatement(connection))
                        .execute())
                .thenReturn(commitSnapshot));
    }

    @Override
    public Flux<CommitSnapshot> insert(final Collection<CommitSnapshot> commitSnapshots) {
        if (commitSnapshots.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = insertStatement(connection);
            commitSnapshots.forEach(snapshot -> applyInsertBindings(snapshot, statement).add());
            statement.fetchSize(commitSnapshots.size());

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(commitSnapshots));
        });
    }

    @Override
    public Flux<CommitSnapshot> mapToPulls(final Collection<CommitSnapshot> commitSnapshots) {
        if (commitSnapshots.isEmpty()) {
            return Flux.empty();
        }

        if (commitSnapshots.stream().anyMatch(commitSnapshot -> commitSnapshot.getPullId() == null)) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = (PostgresqlStatement) connection.createStatement("""
                    INSERT INTO commit_snapshot_pull (
                    commit_snapshot_sha, commit_snapshot_repo_id, pull_id)
                    VALUES ($1, $2, $3)
                    ON CONFLICT (commit_snapshot_sha, commit_snapshot_repo_id, pull_id) DO NOTHING
                    """);
            commitSnapshots.forEach(commitSnapshot -> {
                final var snapId = commitSnapshot.getId();
                statement.bind("$1", snapId.getSha())
                        .bind("$2", snapId.getRepoId())
                        .bind("$3", Objects.requireNonNull(commitSnapshot.getPullId()))
                        .add();
            });
            statement.fetchSize(commitSnapshots.size());

            return statement.execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(commitSnapshots));
        });
    }

    @Override
    public Mono<CommitSnapshot> findById(final CommitSnapshot.Id id) {
        return withConnection(connection -> Mono
                .from(applySelectBindings(id, selectStatement(connection))
                        .execute())
                .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert)));
    }

    @Override
    public Flux<CommitSnapshot> findById(final Collection<CommitSnapshot.Id> ids) {
        return manyWithConnection(connection -> {
            final var statement = selectStatement(connection);
            ids.forEach(id -> applySelectBindings(id, statement).add());
            statement.fetchSize(ids.size());

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.convert(result, this::convert));
        });
    }

    private static PostgresqlStatement insertStatement(final Connection connection) {
        return (PostgresqlStatement) connection
                .createStatement("""
                        INSERT INTO commit_snapshot (sha, repo_id, branch)
                        VALUES ($1, $2, $3)
                        ON CONFLICT (sha, repo_id) DO NOTHING
                        """);
    }

    private static PostgresqlStatement applyInsertBindings(final CommitSnapshot commitSnapshot, final PostgresqlStatement statement) {
        return statement
                .bind("$1", commitSnapshot.getSha())
                .bind("$2", commitSnapshot.getRepo().getId())
                .bind("$3", commitSnapshot.getBranch());
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
                        FROM commit_snapshot snap
                           JOIN repo_github repo
                               ON snap.repo_id = repo.id
                           JOIN user_github repo_owner
                               ON repo.owner_id = repo_owner.id 
                        WHERE snap.sha = $1 AND snap.repo_id = $2
                        """);
    }

    private static PostgresqlStatement applySelectBindings(final CommitSnapshot.Id id, final PostgresqlStatement statement) {
        return statement
                .bind("$1", id.getSha())
                .bind("$2", id.getRepoId());
    }

    private CommitSnapshot convert(final Row row) {
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
