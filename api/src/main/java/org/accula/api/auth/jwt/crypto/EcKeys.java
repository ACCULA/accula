package org.accula.api.auth.jwt.crypto;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Function;

/**
 * This class reads EC public and private keys using path provided.
 *
 * @author Anton Lamtev
 */
@SuppressWarnings("NullableProblems")
public final class EcKeys {
    private EcKeys() {
    }

    @SneakyThrows
    private static <T extends Key, U extends KeySpec> T key(final Path keyPath,
                                                            final Function<byte[], U> specProducer,
                                                            final BiFunction<KeyFactory, U, T> keyProducer) {
        final var bytes = Files.readAllBytes(keyPath);
        final var spec = specProducer.apply(bytes);
        final var factory = KeyFactory.getInstance("EC");

        return keyProducer.apply(factory, spec);
    }

    public static ECPrivateKey privateKey(final Path path) {
        return (ECPrivateKey) key(path, PKCS8EncodedKeySpec::new, KeyFactory::generatePrivate);
    }

    public static ECPublicKey publicKey(final Path path) {
        return (ECPublicKey) key(path, X509EncodedKeySpec::new, KeyFactory::generatePublic);
    }

    @FunctionalInterface
    private interface BiFunction<T, U, R> {
        R apply(T p1, U p2) throws Exception;
    }
}
