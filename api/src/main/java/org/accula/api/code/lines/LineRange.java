package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.stream.IntStream;

/**
 * @author Anton Lamtev
 */
public interface LineRange extends Comparable<LineRange>, IntIterable {
    static LineRange of(final int from, final int to) {
        if (from <= 0) {
            throw new IllegalArgumentException("from = %d MUST be positive".formatted(from));
        }
        if (to < from) {
            throw new IllegalArgumentException("to = %d MUST be >= from = %d".formatted(to, from));
        }
        if (from == to) {
            return of(from);
        }
        return new FromTo(from, to);
    }

    static LineRange of(final int line) {
        if (line <= 0) {
            throw new IllegalArgumentException("line = %d MUST be positive".formatted(line));
        }
        if (line <= Single.Cache.LINES.length) {
            return Single.Cache.LINES[line - 1];
        }
        return new Single(line);
    }

    static LineRange until(final int to) {
        return of(1, to);
    }

    int from();

    int to();

    default int count() {
        return to() - from() + 1;
    }

    boolean contains(int line);

    boolean contains(LineRange lines);

    boolean containsAny(LineRange lines);

    default int compareTo(final int line) {
        if (contains(line)) {
            return 0;
        }
        if (from() > line) {
            return 1;
        }
        return -1;
    }

    @Override
    default int compareTo(final LineRange other) {
        if (from() > other.to()) {
            // this :     ---
            // other: ---
            return 1;
        }
        if (to() < other.from()) {
            // this : ---
            // other:     ---
            return -1;
        }
        // this : ---   |   ---
        // other:   --- | ---
        return 0;
    }

    default int strictlyCompareTo(final LineRange other) {
        if (contains(other)) {
            return 0;
        }
        if (from() > other.to()) {
            return 1;
        }
        return -1;
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class FromTo implements LineRange {
        int from;
        int to;

        @Override
        public boolean contains(final int line) {
            return from <= line && line <= to;
        }

        @Override
        public boolean contains(final LineRange lines) {
            return from <= lines.from() && lines.to() <= to;
        }

        @Override
        public boolean containsAny(final LineRange lines) {
            return from <= lines.to() && to >= lines.from();
        }

        @Override
        public String toString() {
            return "[" + from + ":" + to + "]";
        }

        @Override
        public IntIterator iterator() {
            return new Iterator(from, to);
        }

        static final class Iterator implements IntIterator {
            private final IntIterator iter;

            Iterator(final int from, final int to) {
                iter = IntIterators.fromTo(from - 1, to);
            }

            @Override
            public int nextInt() {
                return iter.nextInt() + 1;
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }
        }
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Single implements LineRange {
        public static final String CACHE_SIZE_PROPERTY_KEY = Cache.class.getName().replace("$", ".") + ".size";
        int line;

        @Override
        public int from() {
            return line;
        }

        @Override
        public int to() {
            return line;
        }

        @Override
        public int count() {
            return 1;
        }

        @Override
        public boolean contains(final int line) {
            return this.line == line;
        }

        @Override
        public boolean contains(final LineRange lines) {
            return line == lines.from() && line == lines.to();
        }

        @Override
        public boolean containsAny(final LineRange lines) {
            return lines.from() <= line && line <= lines.to();
        }

        @Override
        public String toString() {
            return "[" + line + "]";
        }

        @Override
        public int compareTo(final int line) {
            return this.line - line;
        }

        @Override
        public IntIterator iterator() {
            return IntIterators.singleton(line);
        }

        private static final class Cache {
            private static final LineRange[] LINES;

            static {
                int cacheSize = 0;
                try {
                    cacheSize = Math.max(Integer.parseInt(System.getProperty(CACHE_SIZE_PROPERTY_KEY, "0")), 0);
                } catch (NumberFormatException ignored) {
                }
                LINES = IntStream
                        .rangeClosed(1, cacheSize)
                        .mapToObj(Single::new)
                        .toArray(LineRange[]::new);
            }

            private Cache() {
            }
        }
    }
}
