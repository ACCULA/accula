package org.accula.api.util;

import reactor.function.Function4;
import reactor.function.Function6;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class Lambda {
    private static final BiFunction<Object, Object, Object> FIRST_ARG = (fst, snd) -> fst;
    private static final Function<Object, Object> IDENTITY = arg -> arg;

    private Lambda() {
    }

    public static <T1, T2, T3, Last, R>
    Function<Tuple3<T1, T2, T3>, R> passingTailArg(final Function4<T1, T2, T3, Last, R> f4, final Last last) {
        return tuple -> f4.apply(tuple.getT1(), tuple.getT2(), tuple.getT3(), last);
    }

    public static <T1, T2, T3, T4, Last1, Last2, R>
    Function<Tuple4<T1, T2, T3, T4>, R> passingTailArgs(final Function6<T1, T2, T3, T4, Last1, Last2, R> f6,
                                                        final Last1 last1,
                                                        final Last2 last2) {
        return tuple -> f6.apply(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), last1, last2);
    }

    public static <T1, T2, R> Function<Tuple2<T1, T2>, R> passingFirstArg(final Function<T1, R> f) {
        return tuple -> f.apply(tuple.getT1());
    }

    public static <T, R> Function<T, R> expandingWithArg(final Supplier<R> noArgFun) {
        return arg -> noArgFun.get();
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2> BiFunction<T1, T2, T1> firstArg() {
        return (BiFunction<T1, T2, T1>) FIRST_ARG;
    }

    public static <T1, T2, R> BiFunction<T1, T2, R> firstArg(final Function<T1, R> keyPath) {
        return (fst, snd) -> keyPath.apply(fst);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> identity() {
        return (Function<T, T>) IDENTITY;
    }
}
