package org.accula.api.code.git.diff;

import com.google.common.collect.Iterators;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.lines.LineRange;
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

    @Test
    void testBothRenameAndEdit() {
        final var diff = """
            diff --git a/f1 b/f2
            similarity index 66%
            rename from f1
            rename to f2
            index 172d58a..cbaface 100644
            --- a/f1
            +++ b/f2
            @@ -1,3 +1,3 @@
             xxx
            -yyy
            +xyz
             zzz
            """;
        final var parser = new FileDiffParser();
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "cbaface",
                    "f2"
                ),
                LineSet.of(LineRange.of(2))
            ),
            parser.parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }
}
