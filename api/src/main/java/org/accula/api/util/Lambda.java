package org.accula.api.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class Lambda {
    private static final BiFunction<Object, Object, Object> FIRST_ARG = (fst, __) -> fst;
    private static final BiConsumer<Object, Object> ILLEGAL_STATE = (__, ___) -> { throw new IllegalStateException(); };

    private Lambda() {
    }

    public static <T, Last, R> Function<T, R> passingTailArg(final BiFunction<T, Last, R> f2, final Last last) {
        return t -> f2.apply(t, last);
    }

    public static <First, T, R> Function<T, R> passingFirstArg(final BiFunction<First, T, R> f2, final First first) {
        return t -> f2.apply(first, t);
    }

    public static <T, R> Function<T, R> expandingWithArg(final Supplier<R> noArgFun) {
        return arg -> noArgFun.get();
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2> BiFunction<T1, T2, T1> firstArg() {
        return (BiFunction<T1, T2, T1>) FIRST_ARG;
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2> BiConsumer<T1, T2> illegalState() {
        return (BiConsumer<T1, T2>) ILLEGAL_STATE;
    }
}
