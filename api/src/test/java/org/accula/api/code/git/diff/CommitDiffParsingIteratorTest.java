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
            diff --git a/binfile b/binfile
            new file mode 100644
            index 0000000..c5c01cf
            Binary files /dev/null and b/binfile differ
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
                            "gradle/wrapper/gradle-wrapper.jar"
                        ),
                        LineSet.empty()
                    ),
                    FileDiff.of(
                        GitFile.of(
                            "c5c01cf",
                            "binfile"
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
    void testEmptyFiles() {
        var diff = """
            d59787aea288f9f5607f4dbab908b37d022c4ade scripts
            diff --git a/src/main/java/ru/mail/polis/service/ogamoga/ServiceImpl.java b/src/main/java/ru/mail/polis/service/ogamoga/ServiceImpl.java
            new file mode 100644
            index 0000000..52f83a7
            --- /dev/null
            +++ b/src/main/java/ru/mail/polis/service/ogamoga/ServiceImpl.java
            @@ -0,0 +1,9 @@
            +package ru.mail.polis.service.ogamoga;
            +
            +import ru.mail.polis.lsm.DAO;
            +import ru.mail.polis.service.Service;
            +
            +public class ServiceImpl implements Service {
            +    public ServiceImpl(int port, DAO dao) {
            +    }
            +}
            diff --git a/src/test/java/ru/mail/polis/lsm/ReverseTest.java b/src/test/java/ru/mail/polis/lsm/ReverseTest.java
            new file mode 100644
            index 0000000..a4bad25
            --- /dev/null
            +++ b/src/test/java/ru/mail/polis/lsm/ReverseTest.java
            @@ -0,0 +1,12 @@
            +package ru.mail.polis.lsm;
            +
            +public class ReverseTest {
            +    @Test
            +    void emptyReverse(@TempDir Path data) throws IOException {
            +        try (DAO dao = TestDaoWrapper.create(new DAOConfig(data))) {
            +            ByteBuffer notExistedKey = ByteBuffer.wrap("NOT_EXISTED_KEY".getBytes(StandardCharsets.UTF_8));
            +            Iterator<Record> shouldBeEmptyReverse = dao.descendingRange(notExistedKey, null);
            +            assertFalse(shouldBeEmptyReverse.hasNext());
            +        }
            +    }
            +}
            diff --git a/wrk/get.lua b/wrk/get.lua
            new file mode 100644
            index 0000000..e69de29
            diff --git a/wrk/put.lua b/wrk/put.lua
            new file mode 100644
            index 0000000..e69de29
            e673478ed2102c2f6bfe72628f172c386a5080dc [stage-1] - fixed * in imports
            diff --git a/src/main/java/ru/mail/polis/lsm/LsmDAO.java b/src/main/java/ru/mail/polis/lsm/LsmDAO.java
            index 3161084..202daf9 100644
            --- a/src/main/java/ru/mail/polis/lsm/LsmDAO.java
            +++ b/src/main/java/ru/mail/polis/lsm/LsmDAO.java
            @@ -6,7 +6,12 @@ import java.io.IOException;
             import java.io.UncheckedIOException;
             import java.nio.ByteBuffer;
             import java.nio.file.Path;
            -import java.util.*;
            +import java.util.ArrayList;
            +import java.util.Collections;
            +import java.util.Iterator;
            +import java.util.List;
            +import java.util.NavigableMap;
            +import java.util.SortedMap;
             import java.util.concurrent.ConcurrentLinkedDeque;
             import java.util.concurrent.ConcurrentSkipListMap;
            \s
            """;
        final var iter = new CommitDiffParsingIterator(diff.lines().iterator());
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "d59787aea288f9f5607f4dbab908b37d022c4ade",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "52f83a7",
                            "src/main/java/ru/mail/polis/service/ogamoga/ServiceImpl.java"
                        ),
                        LineSet.inRange(LineRange.until(9))
                    ),
                    FileDiff.of(
                        GitFile.of(
                            "a4bad25",
                            "src/test/java/ru/mail/polis/lsm/ReverseTest.java"
                        ),
                        LineSet.inRange(LineRange.until(12))
                    ),
                    FileDiff.of(
                        GitFile.of(
                            "e69de29",
                            "wrk/get.lua"
                        ),
                        LineSet.empty()
                    ),
                    FileDiff.of(
                        GitFile.of(
                            "e69de29",
                            "wrk/put.lua"
                        ),
                        LineSet.empty()
                    )
                )
            ),
            iter.next()
        );
        assertTrue(iter.hasNext());
        assertEquals(
            new CommitDiff(
                "e673478ed2102c2f6bfe72628f172c386a5080dc",
                List.of(
                    FileDiff.of(
                        GitFile.of(
                            "202daf9",
                            "src/main/java/ru/mail/polis/lsm/LsmDAO.java"
                        ),
                        LineSet.inRange(LineRange.of(9, 14))
                    )
                )
            ),
            iter.next()
        );
        assertFalse(iter.hasNext());
    }
}
