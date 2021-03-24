package org.accula.api.util;

import reactor.function.Function3;
import reactor.util.function.Tuple2;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class Lambda {
    private static final BiFunction<Object, Object, Object> FIRST_ARG = (fst, snd) -> fst;

    private Lambda() {
    }

    public static <T, Last, R> Function<T, R> passingTailArg(final BiFunction<T, Last, R> f2, final Last last) {
        return t -> f2.apply(t, last);
    }

    public static <T1, T2, Last, R> Function<Tuple2<T1, T2>, R> passingTailArg(final Function3<T1, T2, Last, R> f3, final Last last) {
        return tuple -> f3.apply(tuple.getT1(), tuple.getT2(), last);
    }

    public static <T1, T2, R> Function<T2, R> passingFirstArg(final BiFunction<T1, T2, R> f, final T1 t1) {
        return t2 -> f.apply(t1, t2);
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
}
