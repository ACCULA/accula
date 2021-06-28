package org.accula.api.code;

import org.accula.api.db.model.CodeLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anton Lamtev
 */
class LanguagesTest {
    @Test
    void testFilterEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Languages.filter(List.of()));
    }

    @Test
    void testFilterMain() {
        final var actualFilter = Languages.filter(List.of(CodeLanguage.values()));
        assertSame(JvmFileFilter.JVM_MAIN, actualFilter);
    }

    @Test
    void testFilterJava() {
        final var actualFilter = Languages.filter(List.of(CodeLanguage.JAVA));
        assertSame(JvmFileFilter.JAVA_MAIN, actualFilter);
    }

    @Test
    void testFilterKotlin() {
        final var actualFilter = Languages.filter(List.of(CodeLanguage.KOTLIN));
        assertSame(JvmFileFilter.KOTLIN_MAIN, actualFilter);
    }
}
