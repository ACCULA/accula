package org.accula.api.db;

import io.r2dbc.pool.ConnectionPool;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
//@RequiredArgsConstructor
final class PullRepo {
    private final DatabaseClient database;
    private final ConnectionPool pool;

    public PullRepo(DatabaseClient database, ConnectionPool pool) {
        this.database = database;
        this.pool = pool;
    }

    void f() {
//        pool.create().flatMap(connection -> connection.createBatch().)
    }
}
