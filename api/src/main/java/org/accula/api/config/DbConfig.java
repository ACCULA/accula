package org.accula.api.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.codec.EnumCodec;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.model.Project;
import org.accula.api.db.repo.ConnectionProvidedRepo;
import org.accula.api.db.repo.type.Enums;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * @author Anton Lamtev
 */
@Configuration
@EnableR2dbcRepositories
@EnableConfigurationProperties(DbProperties.class)
@RequiredArgsConstructor
public class DbConfig extends AbstractR2dbcConfiguration {
    private final DbProperties dbProperties;

    @Bean
    @Override
    public ConnectionPool connectionFactory() {
        final var pool = dbProperties.getPool();

        final var connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(dbProperties.getHost())
                .port(dbProperties.getPort())
                .username(dbProperties.getUser())
                .password(dbProperties.getPassword())
                .database(dbProperties.getDatabase())
                .codecRegistrar(EnumCodec.builder()
                        .withEnum(Enums.PROJECT_STATE, Project.State.class)
                        .build())
                .build());

        final var poolConfig = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                .initialSize(pool.getMinSize())
                .maxSize(pool.getMaxSize())
                .maxIdleTime(pool.getMaxIdleTime())
                .build();

        return new ConnectionPool(poolConfig);
    }

    @Bean
    public ConnectionProvidedRepo.ConnectionProvider connectionProvider(final ConnectionPool connectionPool) {
        return connectionPool::create;
    }
}
