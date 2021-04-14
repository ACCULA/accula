package org.accula.api.handler.signature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anton Lamtev
 */
class SignaturesTest {
    @Test
    void testBadAlgo() {
        assertThrows(Exception.class, () -> {
            final var mac = Signatures.class.getDeclaredMethod("mac", String.class, String.class);
            mac.setAccessible(true);
            mac.invoke(null, "some secret", "non existent algo");
        });
    }
}
