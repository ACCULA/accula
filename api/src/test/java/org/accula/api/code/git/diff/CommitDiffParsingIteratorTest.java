package org.accula.api.code.git.diff;

import org.accula.api.code.git.CommitDiff;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
class CommitDiffParsingIteratorTest {
    @Test
    void testIteratorContract() {
        var iter = new CommitDiffParsingIterator("".lines().iterator());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    void testInvalidInput() {
        var iter = new CommitDiffParsingIterator("something".lines().iterator());
        assertFalse(iter.hasNext());
    }

    @Test
    void testNoAdditionsCase() {
        var diff = """
            eac1afb872a8a802b50b6eef92629fa2a026ee7e Remove extra line
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        Assertions.assertEquals(
            new CommitDiff(
                "eac1afb872a8a802b50b6eef92629fa2a026ee7e",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "5b994b0",
                            "src/main/java/ru/mail/polis/service/Value.java"
                        ),
                        LineSet.empty()
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testCaseWithBinaryFiles() {
        var diff = """
            4ba6786323dc3440fe278691248f230f392fc885 add all
            diff --git a/gradle/wrapper/gradle-wrapper.jar b/gradle/wrapper/gradle-wrapper.jar
            new file mode 100644
            index 0000000..5c2d1cf
            Binary files /dev/null and b/gradle/wrapper/gradle-wrapper.jar differ
            """;
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "4ba6786323dc3440fe278691248f230f392fc885",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "5c2d1cf",
                            "b/gradle/wrapper/gradle-wrapper.jar"
                        ),
                        LineSet.all()
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testThreeWayMergeCase() {
        var diff = """
            24b59ea9175ec3b9ea50d51edd691b0d6cc8bb53 Merge remote-tracking branch 'mainframe/hash' into hash
                            
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "24b59ea9175ec3b9ea50d51edd691b0d6cc8bb53",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "2ad1e5a",
                            "b/src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java"
                        ),
                        LineSet.of(LineRange.of(43))
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testOneMoreThreeWayMergeCase() {
        var diff = """
            93c573238ace9b217bdb2080e4a90cf3c4d23efb Merge branch 'master' of github.com:polis-mail-ru/2020-highload-dht into stage4
                            
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "93c573238ace9b217bdb2080e4a90cf3c4d23efb",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "28044f2",
                            "src/main/java/ru/mail/polis/service/ServiceFactory.java"
                        ),
                        LineSet.of(LineRange.of(21))
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testRenameWithSimilarity100() {
        var diff = """
            06a7e9f1f8c3391dcb68356617964ce76d695eb8 Эм, все как обычно, валимся перед дедлайном. попытка фикса n1.
            diff --git a/src/main/java/ru/mail/polis/DAOImpl.java b/src/main/java/ru/mail/polis/valaubr/DAOImpl.java
            similarity index 100%
            rename from src/main/java/ru/mail/polis/DAOImpl.java
            rename to src/main/java/ru/mail/polis/valaubr/DAOImpl.java
            """;
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "06a7e9f1f8c3391dcb68356617964ce76d695eb8",
                List.of()
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testEmptyDiff() {
        var diff = """
            16624d43ddc49c777ef8541406d677efbdc56c4a Merge branch 'master' of https://github.com/polis-mail-ru/2020-db-lsm into task3
                        
            """;
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(new CommitDiff("16624d43ddc49c777ef8541406d677efbdc56c4a", List.of()), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testNewFile() {
        var diff = """
            e3e83f3f73746c4b4d896f3b520e4658aa4b76c0 xxx
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "e3e83f3f73746c4b4d896f3b520e4658aa4b76c0",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "e69de29",
                            "logs/2019-11-23_18-07-47.413679/test_data.log"
                        ),
                        LineSet.of(LineRange.until(4))
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testMultipleHunks() {
        var diff = """
            e3e83f3f73746c4b4d896f3b520e4658aa4b76c0 xxx
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "e3e83f3f73746c4b4d896f3b520e4658aa4b76c0",
                List.of(
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
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testCaseWithNoNewlinesAtEndOfFile() {
        var diff = """
            3c7c78070355f136a054bde726e86081cced4bf0 Task 3
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
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "3c7c78070355f136a054bde726e86081cced4bf0",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "ddee675",
                            "README.md"
                        ),
                        LineSet.inRange(67, 77)
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testYamlThatLooksLikeAThreeWayMergeHunk() {
        var diff = """
            81eee0f8c7b8b201bd70c45a1b2b964818a74415 Fix test ignore
            diff --git a/.codeclimate.yml b/.codeclimate.yml
            index 7bcb5b0..c00c8b4 100644
            --- a/.codeclimate.yml
            +++ b/.codeclimate.yml
            @@ -59,4 +59,4 @@ checks:
                   threshold: 10 # Have pattern-matching-like code
             exclude_patterns:
             - "**/*.svg"
            -- "test/**/*.java"
            +- "src/test/"
            b7e755e5a278c10e19a5181e362c52189cb6775e ammogenerator and testResults
            diff --git a/src/main/java/ru/mail/polis/service/bezrukova/AmmoGenerator.java b/src/main/java/ru/mail/polis/service/bezrukova/AmmoGenerator.java
            new file mode 100644
            index 0000000..e7b224a
            --- /dev/null
            +++ b/src/main/java/ru/mail/polis/service/bezrukova/AmmoGenerator.java
            @@ -0,0 +1,14 @@
            +package ru.mail.polis.service.bezrukova;
            +
            +import java.io.ByteArrayOutputStream;
            +
            +public class AmmoGenerator {
            +    private static final int LENGTH = 256;
            +    private static final Logger logger = Logger.getLogger(AmmoGenerator.class.getName());
            +
            +    private static byte[] randomValue() {
            +        final byte[] result = new byte[LENGTH];
            +        ThreadLocalRandom.current().nextBytes(result);
            +        return result;
            +    }
            +}
            fb1b904c3fe5817d6c8fdab35727ac3d01d9fffb Fixed CodeClimate issues
            """;
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "81eee0f8c7b8b201bd70c45a1b2b964818a74415",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "c00c8b4",
                            ".codeclimate.yml"
                        ),
                        LineSet.inRange(LineRange.of(62))
                    )
                )
            ),
            iter.next()
        );
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "b7e755e5a278c10e19a5181e362c52189cb6775e",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "e7b224a",
                            "src/main/java/ru/mail/polis/service/bezrukova/AmmoGenerator.java"
                        ),
                        LineSet.inRange(1, 14)
                    )
                )
            ),
            iter.next()
        );
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "fb1b904c3fe5817d6c8fdab35727ac3d01d9fffb",
                List.of()
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }

    @Test
    void testQuotedFilenameAtStart() {
        final var diff = """
            81eee0f8c7b8b201bd70c45a1b2b964818a74415 Fix test ignore
            diff --git "a/report/stage1/abc.md" "b/report/stage1/abc.md"
            new file mode 100644
            index 0000000..ed8da6f
            --- /dev/null
            +++ "b/report/stage1/abc.md"
            @@ -0,0 +1,1 @@
            +# Отчёт
            \\ No newline at end of file
            """;
        var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "81eee0f8c7b8b201bd70c45a1b2b964818a74415",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "ed8da6f",
                            "abc.md"
                        ),
                        LineSet.inRange(LineRange.of(1))
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }
}
