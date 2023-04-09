package org.accula.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@ConfigurationProperties("accula.db")
public record DbProperties(String host, int port, String user, String password, String database, Pool pool) {
    public record Pool(Duration maxIdleTime, int minSize, int maxSize) {
    }
}
