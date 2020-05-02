package org.accula.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@ConfigurationProperties("accula.db")
@Data
public final class DbProperties {
    private String host;
    private int port;
    private String user;
    private String password;
    private String database;
    private Pool pool;

    @Data
    public static final class Pool {
        private Duration maxIdleTime;
        private int minSize;
        private int maxSize;
    }
}
