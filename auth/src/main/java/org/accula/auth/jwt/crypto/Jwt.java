package org.accula.auth.jwt.crypto;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Anton Lamtev
 */
public final class Jwt {
    private final Algorithm algorithm;
    private final String issuer;
    private final long expiresInSecs;

    public Jwt(@NotNull final ECPrivateKey privateEcKey,
               @NotNull final ECPublicKey publicEcKey,
               @NotNull final String issuer,
               final int expiresInMins) {
        this.algorithm = Algorithm.ECDSA256(publicEcKey, privateEcKey);
        this.issuer = issuer;
        this.expiresInSecs = SECONDS.convert(expiresInMins, MINUTES);
    }

    @NotNull
    public Details generate(@NotNull final String subject) {
        final var expirationInstant = Instant.now().plusSeconds(expiresInSecs);
        final var token = JWT
                .create()
                .withIssuer(issuer)
                .withExpiresAt(Date.from(expirationInstant))
                .withSubject(subject)
                .sign(algorithm);

        return new Details(token, expirationInstant.toString());
    }

    @NotNull
    public String verify(@NotNull final String jwt) {
        return JWT
                .require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(jwt)
                .getSubject();
    }

    @RequiredArgsConstructor
    public static class Details {
        public final String token;
        public final String expirationDate;
    }
}
