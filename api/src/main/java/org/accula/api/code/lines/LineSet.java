package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterable;

import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface LineSet extends IntIterable {
    static LineSet inRange(final int from, final int to) {
        return inRange(LineRange.of(from, to));
    }

    static LineSet inRange(final LineRange range) {
        return new LineSetInRangeImpl(range);
    }

    static LineSet all() {
        return LineSetAllImpl.instance();
    }

    static LineSet empty() {
        return LineSetEmptyImpl.instance();
    }

    /**
     * @implNote line ranges MUST be ordered ascending and MUST be without intersections
     */
    static LineSet of(final LineRange... lineRanges) {
        return switch (lineRanges.length) {
            case 0 -> empty();
            case 1 -> inRange(lineRanges[0]);
            default -> new LineSetLineRangesImpl(lineRanges);
        };
    }

    /**
     * @see #of(LineRange...)
     */
    static LineSet of(final List<LineRange> lines) {
        return of(lines.toArray(new LineRange[0]));
    }

    boolean contains(int line);

    boolean contains(LineRange lines);

    boolean containsAny(LineRange lines);

    default boolean isEmpty() {
        return false;
    }
}
