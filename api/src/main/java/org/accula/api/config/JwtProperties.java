package org.accula.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("accula.jwt")
@Data
public final class JwtProperties {
    private Signature signature;
    private String issuer;
    private ExpiresIn expiresIn;

    @Data
    public static final class Signature {
        private Path publicKey;
        private Path privateKey;
    }

    @Data
    public static final class ExpiresIn {
        private Duration access;
        private Duration refresh;
    }
}
