package org.accula.api.util;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public final class Iterables {
    private Iterables() {
    }

    public static boolean isEmptyCollection(final Iterable<?> iterable) {
        return iterable instanceof Collection<?> collection && collection.isEmpty();
    }

    public static <T> Iterable<T> withHead(final Iterable<T> iterable, final T head) {
        return () -> Iterators.withHead(iterable.iterator(), head);
    }
}
