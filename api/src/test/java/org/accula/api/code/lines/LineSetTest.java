package org.accula.api.code.lines;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Anton Lamtev
 */
class LineSetTest {
    static LineSet empty = LineSet.of();
    static LineSet all = LineSet.all();
    static LineRange range = LineRange.of(1, 5);
    static LineSet of = LineSet.of(List.of(range));
    static LineSet inRange = LineSet.inRange(range.from(), range.to());
    static LineSet ranges = LineSet.of(
            LineRange.of(1),
            LineRange.of(5, 10),
            LineRange.of(12),
            LineRange.of(15, 40),
            LineRange.of(64, 127),
            LineRange.of(233),
            LineRange.of(255, 512)
    );
    static Iterable<LineRange> contains = List.of(
            LineRange.of(5),
            LineRange.of(6),
            LineRange.of(7),
            LineRange.of(18, 33),
            LineRange.of(100, 105),
            LineRange.of(256, 372)
    );
    static Iterable<LineRange> containsAny = List.of(
            LineRange.of(1, 6),
            LineRange.of(7, 14),
            LineRange.of(100, 128),
            LineRange.of(128, 233),
            LineRange.of(256, 513)
    );
    static Iterable<LineRange> notContains = List.of(
            LineRange.of(2),
            LineRange.of(3),
            LineRange.of(4),
            LineRange.of(2, 4),
            LineRange.of(13, 14),
            LineRange.of(41, 63),
            LineRange.of(128, 232),
            LineRange.of(234, 254),
            LineRange.of(1024, 2048)
    );

    @Test
    void testEmpty() {
        for (var range : LineRangeTest.allRanges) {
            assertFalse(empty.contains(range));
        }
        LineRange.until(Short.MAX_VALUE).iterator()
                .forEachRemaining(line -> assertFalse(empty.contains(line)));
        for (var range : LineRangeTest.allRanges) {
            assertFalse(empty.containsAny(range));
        }
        assertTrue(empty.isEmpty());
        empty.iterator().forEachRemaining(unreachable -> fail());
    }

    @Test
    void testAll() {
        for (var range : LineRangeTest.allRanges) {
            assertTrue(all.contains(range));
        }
        LineRange.until(Short.MAX_VALUE).iterator()
                .forEachRemaining(line -> assertTrue(all.contains(line)));
        for (var range : LineRangeTest.allRanges) {
            assertTrue(all.containsAny(range));
        }
        assertFalse(all.isEmpty());
        all.iterator().forEachRemaining(line -> assertTrue(all.contains(line)));
    }

    @Test
    void testContains() {
        var rangesIterator = ranges.iterator();
        rangesIterator.forEachRemaining(line -> assertTrue(ranges.contains(line)));
        assertFalse(rangesIterator.hasNext());

        for (var range : contains) {
            range.iterator().forEachRemaining(line -> assertTrue(ranges.contains(line)));
        }
        for (var range : notContains) {
            range.iterator().forEachRemaining(line -> assertFalse(ranges.contains(line)));
        }
        for (var range : contains) {
            assertTrue(ranges.contains(range));
        }
        for (var range : containsAny) {
            assertFalse(ranges.contains(range));
        }
        for (var range : notContains) {
            assertFalse(ranges.contains(range));
        }
        assertTrue(of.contains(range));
        assertEquals(of, inRange);
        inRange.iterator().forEachRemaining(line -> assertTrue(of.contains(line)));
    }

    @Test
    void testContainsAny() {
        for (var range : containsAny) {
            assertTrue(ranges.containsAny(range));
        }
        for (var range : notContains) {
            assertFalse(ranges.containsAny(range));
        }
        assertTrue(of.containsAny(range));
    }

    @Test
    void testInRange() {
        var i1 = inRange.iterator();
        var i2 = of.iterator();
        i1.forEachRemaining(line -> assertEquals(line, i2.nextInt()));
    }

    @Test
    void testIterator() {
        var iter = LineSet.of(LineRange.until(5), LineRange.of(10, 15)).iterator();
        assertTrue(iter.hasNext());
        while (iter.hasNext()) iter.nextInt();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);

        var i1 = (PrimitiveIterator.OfInt) LineSet.inRange(1, 15).iterator();
        var i2 = LineSet.of(
                LineRange.until(5),
                LineRange.of(6, 8),
                LineRange.of(9, 12),
                LineRange.of(13),
                LineRange.of(14, 15)).iterator();
        while (i1.hasNext()) {
            assertEquals(i1.nextInt(), i2.nextInt());
        }
        assertFalse(i1.hasNext());
        assertFalse(i2.hasNext());

        i1 = IntStream.rangeClosed(1, Integer.MAX_VALUE).iterator();
        i2 = LineSet.all().iterator();
        while (i1.hasNext()) {
            assertEquals(i1.nextInt(), i2.nextInt());
        }
        assertFalse(i1.hasNext());
        assertFalse(i2.hasNext());
    }

    @Test
    void testToString() {
        assertEquals(
            LineRange.until(10).toString(),
            LineSet.inRange(1, 10).toString()
        );
        assertEquals("[]", LineSet.empty().toString());
        assertEquals("[1, 2, 3, ..., 2147483647]", LineSet.all().toString());
        assertEquals("[[1...4], [6...8]]", LineSet.of(LineRange.until(4), LineRange.of(6, 8)).toString());
    }
}
