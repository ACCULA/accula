package org.accula.api.util;

import reactor.function.Function4;
import reactor.util.function.Tuple3;

import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public final class Lambda {
    private Lambda() {
    }

    public static <T1, T2, T3, Last, R> Function<Tuple3<T1, T2, T3>, R> passingLastArgument(final Function4<T1, T2, T3, Last, R> f4,
                                                                                            final Last last) {
        return tuple -> f4.apply(tuple.getT1(), tuple.getT2(), tuple.getT3(), last);
    }
}
