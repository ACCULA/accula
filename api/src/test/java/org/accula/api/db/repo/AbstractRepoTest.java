package org.accula.api.db.repo;


import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.postgresql.codec.EnumCodec;
import lombok.SneakyThrows;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.type.Enums;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Testcontainers
abstract class AbstractRepoTest {
    @Container
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12.6")
        .withInitScript(initDbScript());

    final ConnectionProvidedRepo.ConnectionProvider connectionProvider() {
        final var conf = PostgresqlConnectionFactoryProvider
            .builder(PostgreSQLR2DBCDatabaseContainer.getOptions(postgres))
            .codecRegistrar(EnumCodec.builder()
                .withEnum(Enums.PROJECT_STATE, Project.State.class)
                .withEnum(Enums.USER_ROLE, User.Role.class)
                .build())
            .build();
        final var poolConfig = ConnectionPoolConfiguration.builder()
            .connectionFactory(new PostgresqlConnectionFactory(conf))
            .initialSize(2)
            .maxSize(150)
            .maxIdleTime(Duration.ofMinutes(1L))
            .build();
        final var pool = new ConnectionPool(poolConfig);

        return pool::create;
    }

    @SneakyThrows
    private static String initDbScript() {
        final var createSqlPath = AbstractRepoTest.class.getClassLoader().getResource("db/migration/V1__create.sql").getFile();
        final var migration = createSqlPath.substring(0, createSqlPath.length() - "/V1__create.sql".length());

        final var migrationLines = Files.walk(Path.of(migration))
            .filter(file -> file.toString().endsWith(".sql"))
            .sorted()
            .flatMap(AbstractRepoTest::lines)
            .collect(Collectors.toSet());

        final var initSql = "init.sql";
        final var initSqlPath = Path.of(AbstractRepoTest.class.getClassLoader().getResource(initSql).getFile());
        final var initSqlLines = lines(initSqlPath).collect(Collectors.toSet());

        if (!migrationLines.equals(initSqlLines)) {
            throw new IllegalStateException("init.sql differs from flyway migration files");
        }

        return initSql;
    }

    @SneakyThrows
    private static Stream<String> lines(final Path path) {
        return Files.lines(path);
    }
}
