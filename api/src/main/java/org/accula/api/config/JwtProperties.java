package org.accula.api.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@ConstructorBinding
@ConfigurationProperties("accula.jwt")
@Value
public class JwtProperties {
    Signature signature;
    String issuer;
    ExpiresIn expiresIn;
    String refreshTokenEndpointPath;

    @Value
    public static class Signature {
        Path publicKey;
        Path privateKey;
    }

    @Value
    public static class ExpiresIn {
        Duration access;
        Duration refresh;
    }
}
