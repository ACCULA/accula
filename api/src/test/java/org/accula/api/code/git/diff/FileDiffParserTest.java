package org.accula.api.code.git.diff;

import com.google.common.collect.Iterators;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.lines.LineSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Lamtev
 */
public class FileDiffParserTest {
    @Test
    void testNoAdditionsCase() {
        final var diff = """
            diff --git a/src/main/java/ru/mail/polis/service/Value.java b/src/main/java/ru/mail/polis/service/Value.java
            index eed8bde..5b994b0 100644
            --- a/src/main/java/ru/mail/polis/service/Value.java
            +++ b/src/main/java/ru/mail/polis/service/Value.java
            @@ -18,7 +18,6 @@ public final class Value {
                         final long timestamp,
                         final ByteBuffer buffer
                 ) {
            -
                     this.isValueDeleted = isValueDeleted;
                     this.timestamp = timestamp;
                     this.buffer = buffer;
                            """;
        final var parser = new FileDiffParser();
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "5b994b0",
                    "src/main/java/ru/mail/polis/service/Value.java"
                ),
                LineSet.empty()
            ),
            parser.parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }
}
