package org.accula.api.util;

import reactor.function.Function4;
import reactor.util.function.Tuple3;

import java.util.function.Function;
import java.util.function.Supplier;

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

    public static <T, R> Function<T, R> expandingWithArg(final Supplier<R> noArgFun) {
        return arg -> noArgFun.get();
    }

    public static <T> T firstArg(final T first, final T second) {
        return first;
    }
}
