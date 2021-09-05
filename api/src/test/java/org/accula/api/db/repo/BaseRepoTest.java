package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import lombok.Getter;
import org.accula.api.db.repo.codecs.Codecs;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.reactivestreams.Publisher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@Testcontainers
abstract class BaseRepoTest implements ConnectionProvidedRepo {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12.6-alpine");
    @Getter(lazy = true, onMethod_ = @Override)
    private final ConnectionProvider connectionProvider = createConnectionProvider();

    @BeforeEach
    @OverridingMethodsMustInvokeSuper
    void setUp() {
        migrate();
    }

    @AfterEach
    @OverridingMethodsMustInvokeSuper
    void tearDown() {
        dropTables();
    }

    final <P extends Publisher<?>> void expectCompleteEmpty(final P publisher) {
        StepVerifier.create(publisher)
            .verifyComplete();
    }

    private void migrate() {
        final var flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .load();
        flyway.migrate();
    }

    private void dropTables() {
        manyWithConnection(connection -> ((PostgresqlStatement) connection.createStatement("""
            DROP TABLE flyway_schema_history;
            DROP TABLE refresh_token;
            DROP TABLE project_repo;
            DROP TABLE project_admin;
            DROP TABLE project_conf;
            DROP TABLE project_excluded_source_author;
            DROP TYPE code_language_enum;
            DROP TABLE project;
            DROP TYPE project_state_enum;
            DROP TABLE user_;
            DROP TYPE user_role_enum;
            DROP TABLE pull_assignee;
            DROP TABLE clone;
            DROP TABLE clone_snippet;
            DROP TABLE snapshot_pull;
            DROP TABLE pull;
            DROP TABLE snapshot;
            DROP TABLE repo_github;
            DROP TABLE user_github;
            DROP TABLE commit;
            """))
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated))
            .then()
            .block();
    }

    private static ConnectionProvider createConnectionProvider() {
        final var postgresConf = PostgresqlConnectionFactoryProvider
            .builder(PostgreSQLR2DBCDatabaseContainer.getOptions(postgres))
            .codecRegistrar(Codecs.enums())
            .build();
        final var poolConf = ConnectionPoolConfiguration.builder()
            .connectionFactory(new PostgresqlConnectionFactory(postgresConf))
            .initialSize(2)
            .maxSize(150)
            .maxIdleTime(Duration.ofMinutes(1L))
            .build();
        final var pool = new ConnectionPool(poolConf);

        return pool::create;
    }
}
