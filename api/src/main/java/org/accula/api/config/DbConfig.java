package org.accula.api.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.accula.api.db.repo.ConnectionProvidedRepo;
import org.accula.api.db.repo.codecs.Codecs;
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
        final var pool = dbProperties.pool();

        final var connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(dbProperties.host())
                .port(dbProperties.port())
                .username(dbProperties.user())
                .password(dbProperties.password())
                .database(dbProperties.database())
                .codecRegistrar(Codecs.enums())
                .build());

        final var poolConfig = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                .initialSize(pool.minSize())
                .maxSize(pool.maxSize())
                .maxIdleTime(pool.maxIdleTime())
                .build();

        return new ConnectionPool(poolConfig);
    }

    @Bean
    public ConnectionProvidedRepo.ConnectionProvider connectionProvider(final ConnectionPool connectionPool) {
        return connectionPool::create;
    }
}
