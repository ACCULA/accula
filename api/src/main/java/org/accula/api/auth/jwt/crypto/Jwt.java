package org.accula.api.auth.jwt.crypto;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.Value;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * @author Anton Lamtev
 */
public final class Jwt {
    private final Algorithm algorithm;
    private final String issuer;

    public Jwt(final ECPrivateKey privateEcKey,
               final ECPublicKey publicEcKey,
               final String issuer) {
        this.algorithm = Algorithm.ECDSA256(publicEcKey, privateEcKey);
        this.issuer = issuer;
    }

    public Details generate(final String subject, final Duration expiresIn) {
        final var expirationDate = Instant.now().plus(expiresIn);
        final var token = JWT
                .create()
                .withIssuer(issuer)
                .withExpiresAt(Date.from(expirationDate))
                .withSubject(subject)
                .sign(algorithm);

        return new Details(token, expirationDate);
    }

    public String verify(final String jwt) {
        return JWT
                .require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(jwt)
                .getSubject();
    }

    @Value
    public static class Details {
        String token;
        Instant expirationDate;
    }
}
