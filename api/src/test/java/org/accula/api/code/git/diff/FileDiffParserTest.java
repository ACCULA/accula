package org.accula.api.code.git.diff;

import com.google.common.collect.Iterators;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        var parser = new FileDiffParser();
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
        var parser = new FileDiffParser();
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

    @Test
    void testThreeWayMergeCase() {
        var diff = """                            
            diff --cc src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java
            index 2acc3c0,5192217..2ad1e5a
            --- a/src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java
            +++ b/src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java
            @@@ -12,9 -17,37 +17,36 @@@ import static org.junit.jupiter.api.Ass
               */
              class HashTableBaseTest {
             \s
            +     // Intentionally non-comparable
            +     static class Key {
            +         final String value;
            +\s
            +         public Key(String value) {
            +             this.value = value;
            +         }
            +\s
            +         @Override
            +         public boolean equals(Object o) {
            +             if (this == o) return true;
            +             if (o == null || getClass() != o.getClass()) return false;
            +             Key key = (Key) o;
            +             return Objects.equals(value, key.value);
            +         }
            +\s
            +         @Override
            +         public int hashCode() {
            +             return Objects.hash(value);
            +         }
            +     }
            +    \s
                  HashTable<String, String> newTable() {
             -        // Use implementation
             -        return null;
             +        return new Table();
                  }
            +    \s
            +     HashTable<Key, String> newStrangeKeyTable() {
            +         // Use implementation
            +         return null;
            +     }
             \s
                  @Test
                  void emptyTable() {
            """;
        var parser = new FileDiffParser();
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "2ad1e5a",
                    "b/src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java"
                ),
                LineSet.of(LineRange.of(43))
            ),
            parser.parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }

    @Test
    void testOneMoreThreeWayMergeCase() {
        var diff = """
            diff --cc src/main/java/ru/mail/polis/service/ServiceFactory.java
            index a390fe9,15f113d..28044f2
            --- a/src/main/java/ru/mail/polis/service/ServiceFactory.java
            +++ b/src/main/java/ru/mail/polis/service/ServiceFactory.java
            @@@ -18,9 -18,9 +18,10 @@@ package ru.mail.polis.service
             \s
              import org.jetbrains.annotations.NotNull;
              import ru.mail.polis.dao.DAO;
             +import ru.mail.polis.service.zvladn7.AsyncService;
             \s
              import java.io.IOException;
            + import java.util.Set;
             \s
              /**
               * Constructs {@link Service} instances.
            """;
        var parser = new FileDiffParser();
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "28044f2",
                    "src/main/java/ru/mail/polis/service/ServiceFactory.java"
                ),
                LineSet.of(LineRange.of(21))
            ),
            parser.parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }

    @Test
    void testRenameWithSimilarity100() {
        var diff = """
            diff --git a/src/main/java/ru/mail/polis/DAOImpl.java b/src/main/java/ru/mail/polis/valaubr/DAOImpl.java
            similarity index 100%
            rename from src/main/java/ru/mail/polis/DAOImpl.java
            rename to src/main/java/ru/mail/polis/valaubr/DAOImpl.java
            """;
        var parser = new FileDiffParser();
        assertNull(parser.parse(Iterators.peekingIterator(diff.lines().iterator())));
    }

    @Test
    void testNewFile() {
        var diff = """
            diff --git a/logs/2019-11-23_18-07-47.413679/test_data.log b/logs/2019-11-23_18-07-47.413679/test_data.log
            new file mode 100644
            index 0000000..e69de29
            --- /dev/null
            +++ b/logs/2019-11-23_18-07-47.413679/test_data.log
            @@ -0,0 +1,4 @@
            +xxx:
            +- x
            +- y
            +
            """;
        var parser = new FileDiffParser();
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "e69de29",
                    "logs/2019-11-23_18-07-47.413679/test_data.log"
                ),
                LineSet.of(LineRange.until(4))
            ),
            parser.parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }

    @Test
    void testMultipleHunks() {
        var diff = """
            diff --git a/src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java b/src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java
            old mode 100755
            new mode 100644
            index 8965001..93a18fe
            --- a/src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java
            +++ b/src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java
            @@ -2,20 +2,24 @@ package ru.mail.polis.service.hljavacourse;
            \s
             import org.jetbrains.annotations.NotNull;
            \s
            -import java.io.*;
            -import java.nio.charset.StandardCharsets;
            +import java.io.ByteArrayOutputStream;
            +import java.io.IOException;
            +import java.io.OutputStreamWriter;
            +import java.io.Writer;
             import java.util.concurrent.ThreadLocalRandom;
            \s
            +import static java.nio.charset.StandardCharsets.US_ASCII;
            +
             public class AmmoGenerator {
            -    private static final int VALUE_LENGTH = 512;
            +    public static final int VALUE_LENGTH = 256;
            \s
                 @NotNull
            -    private static String randomKey(){
            +    private static String randomKey() {
                     return Long.toHexString(ThreadLocalRandom.current().nextLong());
                 }
            \s
                 @NotNull
            -    private static byte[] randomValue(){
            +    private static byte[] randomValue() {
                     final byte[] result = new byte[VALUE_LENGTH];
                     ThreadLocalRandom.current().nextBytes(result);
                     return result;
            @@ -64,4 +64,14 @@ $ git checkout -b part1
               * https://www.e-olymp.com/ru/problems/5327 - скобочки со стеком
               * https://www.e-olymp.com/ru/problems/6124 - стек
            \s
            -За каждое полностью рабочее решение дается 2 балла. \s
            \\ No newline at end of file
            +За каждое полностью рабочее решение дается 2 балла. \s
            +
            +## ДЗ 3.
            +Задачи с e-olymp.com
            +Дэдлайн - 22.10
            +  * https://www.e-olymp.com/ru/problems/3738 - Простая сортировка
            +  * https://www.e-olymp.com/ru/problems/1462 - Хитрая сортировка
            +  * https://www.e-olymp.com/ru/problems/4741 - Сортировка пузырьком
            +  * https://www.e-olymp.com/ru/problems/4827 - k-тая порядковая статистика
            +  * https://www.e-olymp.com/ru/problems/4037 - Сортировка слиянием
            +  * https://www.e-olymp.com/ru/problems/1457 - Станция "Сортировочная"
            """;
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "93a18fe",
                    "src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java"
                ),
                LineSet.of(
                    LineRange.of(5, 8),
                    LineRange.of(11, 12),
                    LineRange.of(14),
                    LineRange.of(17),
                    LineRange.of(22),
                    LineRange.of(67, 77)
                )
            ),
            new FileDiffParser().parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }

    @Test
    void testCaseWithNoNewlinesAtEndOfFile() {
        var diff = """
            diff --git a/README.md b/README.md
            index 654721a..ddee675 100644
            --- a/README.md
            +++ b/README.md
            @@ -64,4 +64,14 @@ $ git checkout -b part1
               * https://www.e-olymp.com/ru/problems/5327 - скобочки со стеком
               * https://www.e-olymp.com/ru/problems/6124 - стек
            \s
            -За каждое полностью рабочее решение дается 2 балла. \s
            \\ No newline at end of file
            +За каждое полностью рабочее решение дается 2 балла. \s
            +
            +## ДЗ 3.
            +Задачи с e-olymp.com
            +Дэдлайн - 22.10
            +  * https://www.e-olymp.com/ru/problems/3738 - Простая сортировка
            +  * https://www.e-olymp.com/ru/problems/1462 - Хитрая сортировка
            +  * https://www.e-olymp.com/ru/problems/4741 - Сортировка пузырьком
            +  * https://www.e-olymp.com/ru/problems/4827 - k-тая порядковая статистика
            +  * https://www.e-olymp.com/ru/problems/4037 - Сортировка слиянием
            +  * https://www.e-olymp.com/ru/problems/1457 - Станция "Сортировочная"
            \\ No newline at end of file
            """;
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "ddee675",
                    "README.md"
                ),
                LineSet.inRange(67, 77)
            ),
            new FileDiffParser().parse(Iterators.peekingIterator(diff.lines().iterator()))
        );
    }

    @Test
    void testQuotedFilename() {
        final var diff = """
            diff --git "a/rep ort/sta ge1/abc.md" "b/rep ort/sta ge1/abc.md"
            new file mode 100644
            index 0000000..ed8da6f
            --- /dev/null
            +++ "b/rep ort/sta ge1/abc.md"
            @@ -0,0 +1,1 @@
            +# Отчёт
            \\ No newline at end of file
            """;
        final var actual = new FileDiffParser().parse(Iterators.peekingIterator(diff.lines().iterator()));
        assertNotNull(actual);
        assertEquals(
            FileDiff.of(
                GitFile.of(
                    "ed8da6f",
                    "rep ort/sta ge1/abc.md"
                ),
                LineSet.inRange(LineRange.of(1))
            ),
            actual
        );
        assertEquals("rep ort/sta ge1/abc.md", actual.file().name());
    }
}
