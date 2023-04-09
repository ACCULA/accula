package org.accula.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@ConfigurationProperties("accula.jwt")
public record JwtProperties(Signature signature, String issuer, ExpiresIn expiresIn, String refreshTokenEndpointPath) {
    public record Signature(Path publicKey, Path privateKey) {
    }

    public record ExpiresIn(Duration access, Duration refresh) {
    }
}
