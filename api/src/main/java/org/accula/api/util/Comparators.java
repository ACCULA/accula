package org.accula.api.util;

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public final class Comparators {
    private Comparators() {
    }

    public static <T,
                   U1 extends Comparable<U1>,
                   U2 extends Comparable<U2>>
    BinaryOperator<T> minBy(final Function<T, U1> comparator, final Function<T, U2> collisionResolvingComparator) {
        return minBy(Comparator.comparing(comparator), Comparator.comparing(collisionResolvingComparator));
    }

    public static <T> BinaryOperator<T> minBy(final Comparator<T> comparator, final Comparator<T> collisionResolvingComparator) {
        return (min, curr) -> {
            final var cmp = comparator.compare(min, curr);
            if (cmp < 0) {
                return min;
            } else if (cmp > 0) {
                return curr;
            } else {
                final var collisionCmp = collisionResolvingComparator.compare(min, curr);
                if (collisionCmp < 0) {
                    return min;
                } else if (collisionCmp > 0) {
                    return curr;
                }
            }
            return min;
        };
    }
}
