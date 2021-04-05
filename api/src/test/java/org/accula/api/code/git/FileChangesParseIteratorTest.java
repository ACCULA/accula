package org.accula.api.code.git;

import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.util.Iterators;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
class FileChangesParseIteratorTest {
    @Test
    void testIteratorContract() {
        final var iter = new FileChangesParseIterator(Iterators.nextResettable("".lines().iterator()));
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    void testInvalidInput() {
        final var iter = new FileChangesParseIterator(Iterators.nextResettable("something".lines().iterator()));
        assertTrue(iter.hasNext());
        assertThrows(IllegalStateException.class, iter::next);
    }

    @Test
    void testNoAdditionsCase() {
        final var diff = """
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
        final var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("eac1afb872a8a802b50b6eef92629fa2a026ee7e Remove extra line", iter.next());
        assertTrue(iter.hasNext());
        assertEquals(GitFileChanges.of(GitFile.of("5b994b0", "src/main/java/ru/mail/polis/service/Value.java"), LineSet.empty()), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testCaseWithBinaryFiles() {
        final var diff = """
                4ba6786323dc3440fe278691248f230f392fc885 add all
                diff --git a/gradle/wrapper/gradle-wrapper.jar b/gradle/wrapper/gradle-wrapper.jar
                new file mode 100644
                index 0000000..5c2d1cf
                Binary files /dev/null and b/gradle/wrapper/gradle-wrapper.jar differ
                """;
        final var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("4ba6786323dc3440fe278691248f230f392fc885 add all", iter.next());
        assertTrue(iter.hasNext());
        assertEquals(GitFileChanges.of(GitFile.of("5c2d1cf", "b/gradle/wrapper/gradle-wrapper.jar"), LineSet.all()), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testThreeWayMergeCase() {
        final var diff = """
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
        final var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("24b59ea9175ec3b9ea50d51edd691b0d6cc8bb53 Merge remote-tracking branch 'mainframe/hash' into hash", iter.next());
        assertTrue(iter.hasNext());
        assertEquals(GitFileChanges.of(GitFile.of("2ad1e5a", "b/src/test/java/ru/mail/polis/ads/hash/HashTableBaseTest.java"), LineSet.of(LineRange.of(43))), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testOneMoreThreeWayMergeCase() {
        final var diff = """
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
        final var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("93c573238ace9b217bdb2080e4a90cf3c4d23efb Merge branch 'master' of github.com:polis-mail-ru/2020-highload-dht into stage4", iter.next());
        assertTrue(iter.hasNext());
        assertEquals(GitFileChanges.of(GitFile.of("28044f2", "src/main/java/ru/mail/polis/service/ServiceFactory.java"), LineSet.of(LineRange.of(21))), iter.next());
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
        var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("06a7e9f1f8c3391dcb68356617964ce76d695eb8 Эм, все как обычно, валимся перед дедлайном. попытка фикса n1.", iter.next());
        assertTrue(iter.hasNext());
        assertSame(GitFileChanges.empty(), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testEmptyDiff() {
        var diff = """
            16624d43ddc49c777ef8541406d677efbdc56c4a Merge branch 'master' of https://github.com/polis-mail-ru/2020-db-lsm into task3
            
            """;
        var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertEquals("16624d43ddc49c777ef8541406d677efbdc56c4a Merge branch 'master' of https://github.com/polis-mail-ru/2020-db-lsm into task3", iter.next());
        assertTrue(iter.hasNext());
        assertSame(GitFileChanges.empty(), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testEndsAtFileId() {
        var diff = """
            diff --git a/logs/2019-11-23_18-07-47.413679/test_data.log b/logs/2019-11-23_18-07-47.413679/test_data.log
            new file mode 100644
            index 0000000..e69de29
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
            """;
        var iter = new FileChangesParseIterator(Iterators.nextResettable(diff.lines().iterator()));
        assertTrue(iter.hasNext());
        assertSame(GitFileChanges.empty(), iter.next());
        assertTrue(iter.hasNext());
        assertEquals(GitFileChanges.of(GitFile.of("93a18fe", "src/main/java/ru/mail/polis/service/hljavacourse/AmmoGenerator.java"),
            LineSet.of(LineRange.of(5, 8), LineRange.of(11, 12), LineRange.of(14), LineRange.of(17), LineRange.of(22))), iter.next());
        assertFalse(iter.hasNext());
    }
}
