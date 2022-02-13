package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author Anton Lamtev
 */
@Value
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
class LineSetLineRangesImpl implements LineSet {
    @Getter(AccessLevel.PRIVATE)
    LineRange[] lineRanges;

    @Override
    public boolean contains(final int line) {
        return binSearch(lineRanges, line) >= 0;
    }

    @Override
    public boolean contains(final LineRange lines) {
        return Arrays.binarySearch(lineRanges, lines, LineRange::strictlyCompareTo) >= 0;
    }

    @Override
    public boolean containsAny(final LineRange lines) {
        return Arrays.binarySearch(lineRanges, lines, LineRange::compareTo) >= 0;
    }

    @Override
    public IntIterator iterator() {
        return new Iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(lineRanges);
    }

    private static int binSearch(final LineRange[] lines, final int line) {
        int low = 0;
        int high = lines.length - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final LineRange midRange = lines[mid];
            final int cmp = midRange.compareTo(line);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    private final class Iterator implements IntIterator {
        private int rangeIndex;
        private IntIterator rangeIterator = lineRanges[rangeIndex].iterator();

        @Override
        public int nextInt() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return rangeIterator.nextInt();
        }

        @Override
        public boolean hasNext() {
            if (rangeIndex >= lineRanges.length - 1) {
                return rangeIterator.hasNext();
            }
            if (!rangeIterator.hasNext()) {
                return (rangeIterator = lineRanges[++rangeIndex].iterator()).hasNext();
            }
            return true;
        }
    }
}
