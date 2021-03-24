package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * @author Anton Lamtev
 */
final class LineSetAllImpl implements LineSet {
    private static final LineSet INSTANCE = new LineSetAllImpl();

    private LineSetAllImpl() {
    }

    static LineSet instance() {
        return INSTANCE;
    }

    @Override
    public boolean contains(final int line) {
        return true;
    }

    @Override
    public boolean contains(final LineRange lines) {
        return true;
    }

    @Override
    public boolean containsAny(final LineRange lines) {
        return true;
    }

    @Override
    public IntIterator iterator() {
        return new LineRange.FromTo.Iterator(1, Integer.MAX_VALUE);
    }
}
