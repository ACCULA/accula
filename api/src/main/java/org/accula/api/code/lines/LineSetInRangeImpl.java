package org.accula.api.code.lines;

import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
class LineSetInRangeImpl implements LineSet {
    LineRange range;

    @Override
    public boolean contains(final int line) {
        return range.contains(line);
    }

    @Override
    public boolean contains(final LineRange lines) {
        return range.contains(lines);
    }

    @Override
    public boolean containsAny(final LineRange lines) {
        return range.containsAny(lines);
    }

    @Override
    public IntIterator iterator() {
        return range.iterator();
    }
}
