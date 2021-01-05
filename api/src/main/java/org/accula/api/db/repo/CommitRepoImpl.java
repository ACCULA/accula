package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Commit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class CommitRepoImpl implements CommitRepo, ConnectionProvidedRepo {
    @Getter
    private final ConnectionProvider connectionProvider;

    @Override
    public Flux<Commit> insert(final Collection<Commit> commits) {
        if (commits.isEmpty()) {
            return Flux.empty();
        }

        return manyWithConnection(connection -> {
            final var statement = BatchStatement.of(connection, """
                    INSERT INTO commit (sha, author_name, author_email, date)
                    VALUES ($collection)
                    ON CONFLICT (sha) DO NOTHING 
                    """);
            statement.bind(commits, commit -> new Object[]{
                    commit.getSha(),
                    commit.getAuthorName(),
                    commit.getAuthorEmail(),
                    commit.getDate()
            });

            return statement
                    .execute()
                    .flatMap(PostgresqlResult::getRowsUpdated)
                    .thenMany(Flux.fromIterable(commits));
        });
    }
}
