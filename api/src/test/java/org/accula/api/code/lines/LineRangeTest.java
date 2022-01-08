package org.accula.api.code.lines;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
class LineRangeTest {
    static LineRange line = LineRange.of(7);
    static LineRange line1 = LineRange.of(3);
    static LineRange line2 = LineRange.of(590);
    static LineRange line3 = LineRange.of(1024);
    static LineRange range = LineRange.of(5, 256);
    static LineRange range1 = LineRange.of(10, 128);
    static LineRange range2 = LineRange.of(1, 5);
    static LineRange range3 = LineRange.of(257, 512);
    static Iterable<LineRange> allLines = List.of(line, line1, line2, line3);
    static Iterable<LineRange> allRanges = List.of(line, line1, line2, line3, range, range1, range2, range3);

    @Test
    void testIllegalInput() {
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(0));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(-1));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(1, -1));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(2, 1));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(0, 1));
    }

    @Test
    void testCount() {
        assertEquals(1, line.count());
        assertEquals(252, range.count());
    }

    @Test
    void testContainsLine() {
        assertTrue(line.contains(line.from()));
        assertTrue(line.contains(line.to()));
        assertFalse(line.contains(line1.from()));
        assertFalse(line.contains(line1.to()));
        range.iterator().forEachRemaining(line -> assertTrue(range.contains(line)));
        range3.iterator().forEachRemaining(line -> assertFalse(range.contains(line)));
    }

    @Test
    void testContains() {
        assertTrue(line.contains(line));
        assertFalse(line.contains(line1));
        assertTrue(line1.contains(line1));
        assertFalse(line1.contains(line2));
        assertTrue(range.contains(line));
        assertTrue(range.contains(range));
        assertFalse(range.contains(line1));
        assertFalse(range.contains(line2));
        assertFalse(range.contains(line3));
        assertTrue(range.contains(range1));
        assertFalse(range.contains(range2));
        assertFalse(range.contains(range3));
        range.iterator().forEachRemaining(line -> {
            final var r = LineRange.of(line);
            assertTrue(r.contains(line));
            assertTrue(r.contains(r));
            assertFalse(r.contains(line + 1));
        });
        for (var line : allLines) {
            assertFalse(line.contains(LineRange.of(line.from(), Integer.MAX_VALUE)));
            line.iterator().forEachRemaining(l -> assertTrue(line.contains(l)));
        }
    }

    @Test
    void testContainsAny() {
        assertTrue(line.containsAny(range));
        assertTrue(range.containsAny(line));
        assertFalse(line1.containsAny(range));
        assertFalse(range.containsAny(line1));
        assertFalse(range.containsAny(line2));
        assertFalse(range.containsAny(line3));
        assertTrue(range.containsAny(range1));
        assertTrue(range1.containsAny(range));
        assertTrue(range.containsAny(range2));
        assertTrue(range2.containsAny(range));
        assertFalse(range.containsAny(range3));
        assertFalse(range3.containsAny(range));
        for (var line : allLines) {
            assertFalse(LineRange.of(Integer.MAX_VALUE).containsAny(line));
        }
    }

    @Test
    void testCompareToLine() {
        assertEquals(0, range.compareTo(line.from()));
        assertTrue(range.compareTo(line1.from()) > 0);
        assertTrue(line1.compareTo(range.from()) < 0);
        assertTrue(range.compareTo(line2.to()) < 0);
        assertTrue(range.compareTo(line3.to()) < 0);
    }

    @Test
    void testCompareTo() {
        assertEquals(0, testCompareTo(range, line));
        assertTrue(testCompareTo(range, line1) > 0);
        assertTrue(testCompareTo(range, line2) < 0);
        assertTrue(testCompareTo(range, line3) < 0);

        assertEquals(0, testCompareTo(range, range1));
        assertEquals(0, testCompareTo(range, range2));
        assertTrue(testCompareTo(range, range3) < 0);
        assertTrue(testCompareTo(range1, range2) > 0);
    }

    @Test
    void testOther() {
        assertEquals(LineRange.of(1), LineRange.of(1, 1));
        assertEquals("[1]", LineRange.of(1).toString());
        assertEquals("[1...2]", LineRange.of(1, 2).toString());
    }

    int testCompareTo(LineRange r1, LineRange r2) {
        var res1 = r1.compareTo(r2);
        var res2 = r2.compareTo(r1);
        assertEquals(Math.abs(res1), Math.abs(res2));
        return res1;
    }
}
