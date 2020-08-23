package org.accula.api.util;

import reactor.core.publisher.Mono;
import reactor.function.Function3;
import reactor.function.TupleUtils;

/**
 * @author Anton Lamtev
 */
public final class RLambda {
    private RLambda() {
    }

    public static <T1, T2, T3, R> Mono<R> zip(final Mono<T1> m1,
                                              final Mono<T2> m2,
                                              final Mono<T3> m3,
                                              final Function3<T1, T2, T3, R> combinator) {
        return Mono
                .zip(m1, m2, m3)
                .map(TupleUtils.function(combinator));
    }
}
