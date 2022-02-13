package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;

/**
 * @author Anton Lamtev
 */
final class LineSetEmptyImpl implements LineSet {
    private static final LineSetEmptyImpl INSTANCE = new LineSetEmptyImpl();

    private LineSetEmptyImpl() {
    }

    static LineSetEmptyImpl instance() {
        return INSTANCE;
    }

    @Override
    public boolean contains(final int line) {
        return false;
    }

    @Override
    public boolean contains(final LineRange lines) {
        return false;
    }

    @Override
    public boolean containsAny(final LineRange lines) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public IntIterator iterator() {
        return IntIterators.EMPTY_ITERATOR;
    }

    @Override
    public String toString() {
        return "[]";
    }
}
