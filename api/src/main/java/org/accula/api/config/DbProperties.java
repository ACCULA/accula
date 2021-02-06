package org.accula.api.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@ConstructorBinding
@ConfigurationProperties("accula.db")
@Value
public class DbProperties {
    String host;
    int port;
    String user;
    String password;
    String database;
    Pool pool;

    @Value
    public static class Pool {
        Duration maxIdleTime;
        int minSize;
        int maxSize;
    }
}
