package org.accula.api.token;

import org.accula.api.code.FileEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public final <Ref> void testEqualMethods(List<Token<Ref>> method1, List<Token<Ref>> method2) {
        assertEquals(toString(method1), toString(method2));
    }

    public final <Ref> Stream<List<Token<Ref>>> methods(FileEntity<Ref> file) {
        return this.<Ref>tokenProvider().tokensByMethods(file);
    }

    private static <Ref> String toString(List<Token<Ref>> method) {
        return method
            .stream()
            .map(Token::string)
            .collect(Collectors.joining(", "));
    }
}
