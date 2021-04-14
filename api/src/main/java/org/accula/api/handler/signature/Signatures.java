package org.accula.api.handler.signature;

import lombok.Generated;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Anton Lamtev
 */
public final class Signatures {
    private static final String HMAC_SHA256 = "HmacSHA256";

    private Signatures() {
    }

    public static boolean checkHexHmacSha256(final String signature, final String secret, final String payload) {
        return signature.equals(signHexHmacSha256(payload, secret));
    }

    public static String signHexHmacSha256(final String payload, final String secret) {
        final var hmacSha256 = mac(secret, HMAC_SHA256);
        return Hex.encodeHexString(hmacSha256.doFinal(payload.getBytes(UTF_8)));
    }

    @SneakyThrows
    private static Mac mac(final String secret, final String algorithm) {
        final var mac = Mac.getInstance(algorithm);
        final var keySpec = new SecretKeySpec(secret.getBytes(UTF_8), algorithm);
        mac.init(keySpec);
        return mac;
    }
}
