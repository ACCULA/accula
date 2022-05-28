package org.accula.api.db.repo;

import com.google.common.collect.Iterables;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.Snapshot;
import org.intellij.lang.annotations.Language;
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
    public Flux<Snapshot> insert(final Iterable<Snapshot> snapshots) {
        if (Iterables.isEmpty(snapshots)) {
            return Flux.empty();
        }

        return transactionalMany(connection -> {
            final var commits = Iterables.transform(snapshots, Snapshot::commit);
            return insertCommits(commits, connection)
                    .thenMany(insert(snapshots, connection));
        });
    }

    @Override
    public Flux<Snapshot> findById(final Collection<Snapshot.Id> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }
        return manyWithConnection(connection -> {
            final var statement = selectByIdStatement(connection);
            StatementUtils.bindIterable(ids, statement, SnapshotRepoImpl::applySelectByIdBindings);

            return statement
                    .execute()
                    .flatMap(result -> ConnectionProvidedRepo.convertMany(result, SnapshotRepoImpl::convert));
        });
    }

    @Override
    public Flux<Snapshot> findByRepoId(final Long repoId) {
        return manyWithConnection(connection ->
                selectByRepoStatement(connection)
                        .bind("$1", repoId)
                        .execute()
                        .flatMap(result -> ConnectionProvidedRepo.convertMany(result, SnapshotRepoImpl::convert)));
    }

    @Override
    public Flux<Snapshot> findByPullId(final Long pullId) {
        return manyWithConnection(connection ->
                selectByPullStatement(connection)
                        .bind("$1", pullId)
                        .execute()
                        .flatMap(result -> ConnectionProvidedRepo.convertMany(result, SnapshotRepoImpl::convert)));
    }

    private static PostgresqlStatement selectByIdStatement(final Connection connection) {
        return selectStatement(connection, "WHERE snap.sha = $1 AND snap.repo_id = $2");
    }

    private static PostgresqlStatement selectByRepoStatement(final Connection connection) {
        return selectStatement(connection, "WHERE repo.id = $1");
    }

    private static PostgresqlStatement selectByPullStatement(final Connection connection) {
        @Language("SQL") final var sql = """
                SELECT commit.sha          AS commit_sha,
                       commit.is_merge     AS commit_is_merge,
                       commit.author_name  AS commit_author_name,
                       commit.author_email AS commit_author_email,
                       commit.date         AS commit_date,
                       snap.branch         AS snap_branch,
                       repo.id             AS repo_id,
                       repo.name           AS repo_name,
                       repo.is_private     AS repo_is_private,
                       repo.description    AS repo_description,
                       repo_owner.id       AS repo_owner_id,
                       repo_owner.login    AS repo_owner_login,
                       repo_owner.name     AS repo_owner_name,
                       repo_owner.avatar   AS repo_owner_avatar,
                       repo_owner.is_org   AS repo_owner_is_org
                FROM snapshot snap
                   JOIN commit
                       ON snap.sha = commit.sha
                   JOIN repo_github repo
                       ON snap.repo_id = repo.id
                   JOIN user_github repo_owner
                       ON repo.owner_id = repo_owner.id
                   JOIN snapshot_pull
                       ON snap.sha = snapshot_pull.snapshot_sha
                            AND snap.repo_id = snapshot_pull.snapshot_repo_id
                            AND snap.branch = snapshot_pull.snapshot_branch
                WHERE snapshot_pull.pull_id = $1
                """;
        return (PostgresqlStatement) connection.createStatement(sql);
    }

    private static PostgresqlStatement selectStatement(final Connection connection, final String whereClause) {
        @Language("SQL") final var sql = """
                SELECT commit.sha          AS commit_sha,
                       commit.is_merge     AS commit_is_merge,
                       commit.author_name  AS commit_author_name,
                       commit.author_email AS commit_author_email,
                       commit.date         AS commit_date,
                       snap.branch         AS snap_branch,
                       repo.id             AS repo_id,
                       repo.name           AS repo_name,
                       repo.is_private     AS repo_is_private,
                       repo.description    AS repo_description,
                       repo_owner.id       AS repo_owner_id,
                       repo_owner.login    AS repo_owner_login,
                       repo_owner.name     AS repo_owner_name,
                       repo_owner.avatar   AS repo_owner_avatar,
                       repo_owner.is_org   AS repo_owner_is_org
                FROM snapshot snap
                   JOIN commit
                       ON snap.sha = commit.sha
                   JOIN repo_github repo
                       ON snap.repo_id = repo.id
                   JOIN user_github repo_owner
                       ON repo.owner_id = repo_owner.id
                """;
        return (PostgresqlStatement) connection.createStatement(String.format("%s %s", sql, whereClause));
    }

    private static PostgresqlStatement applySelectByIdBindings(final Snapshot.Id id, final PostgresqlStatement statement) {
        return statement
                .bind("$1", id.sha())
                .bind("$2", id.repoId());
    }

    private static Snapshot convert(final Row row) {
        return Converters.convertSnapshot(row,
                "commit_sha",
                "commit_is_merge",
                "commit_author_name",
                "commit_author_email",
                "commit_date",
                "snap_branch",
                Converters.NOTHING,
                Converters.NOTHING,
                "repo_id",
                "repo_name",
                "repo_is_private",
                "repo_description",
                "repo_owner_id",
                "repo_owner_login",
                "repo_owner_name",
                "repo_owner_avatar",
                "repo_owner_is_org");
    }

    private static Flux<Commit> insertCommits(final Iterable<Commit> commits, final Connection connection) {
        final var statement = BatchStatement.of(connection, """
                INSERT INTO commit (sha, is_merge, author_name, author_email, date)
                VALUES ($collection)
                ON CONFLICT (sha) DO NOTHING
                """);
        statement.bind(commits, commit -> Bindings.of(
                commit.sha(),
                commit.isMerge(),
                commit.authorName(),
                commit.authorEmail(),
                commit.date()
        ));

        return statement
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .thenMany(Flux.fromIterable(commits));
    }

    private static Flux<Snapshot> insert(final Iterable<Snapshot> snapshots, final Connection connection) {
        final var statement = BatchStatement.of(connection, """
                INSERT INTO snapshot (sha, repo_id, branch)
                VALUES ($collection)
                ON CONFLICT (sha, repo_id, branch) DO NOTHING
                """);
        statement.bind(snapshots, commitSnapshot -> Bindings.of(
                commitSnapshot.sha(),
                commitSnapshot.repo().id(),
                commitSnapshot.branch()
        ));

        return statement
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .thenMany(Flux.fromIterable(snapshots));
    }
}
