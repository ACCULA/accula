package org.accula.api.db.repo;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.postgresql.codec.EnumCodec;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.type.Enums;
import org.flywaydb.core.Flyway;
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
abstract class BaseRepoTest {
    @Container
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12.6-alpine");

    @BeforeEach
    @OverridingMethodsMustInvokeSuper
    void setUp() {
        migrate();
    }

    final ConnectionProvidedRepo.ConnectionProvider connectionProvider() {
        final var postgresConf = PostgresqlConnectionFactoryProvider
            .builder(PostgreSQLR2DBCDatabaseContainer.getOptions(postgres))
            .codecRegistrar(EnumCodec.builder()
                .withEnum(Enums.PROJECT_STATE, Project.State.class)
                .withEnum(Enums.USER_ROLE, User.Role.class)
                .build())
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

    final <P extends Publisher<?>> void expectCompleteEmpty(final P publisher) {
        StepVerifier.create(publisher)
            .expectComplete()
            .verify();
    }

    private void migrate() {
        final var flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .load();
        flyway.migrate();
    }
}
