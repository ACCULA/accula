package org.accula.api.token;

import org.accula.api.code.FileEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
public abstract class BaseLanguageTokenProviderTest {
    public abstract <Ref> LanguageTokenProvider<Ref> tokenProvider();

    public final <Ref> void testMethodCount(FileEntity<Ref> file, int expectedMethodCount) {
        assertEquals(expectedMethodCount, this.<Ref>tokenProvider().tokensByMethods(file).count());
    }

    public final <Ref> void testSupportsFile(FileEntity<Ref> file) {
        assertTrue(this.<Ref>tokenProvider().supportsFile(file));
    }

    public final <Ref> void testDoesNotSupportFile(FileEntity<Ref> file) {
        assertFalse(this.<Ref>tokenProvider().supportsFile(file));
    }
}
