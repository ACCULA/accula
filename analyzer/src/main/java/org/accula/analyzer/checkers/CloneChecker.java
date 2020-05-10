package org.accula.analyzer.checkers;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface CloneChecker <T, U> {
    Mono<U> checkClones(final T a, final T b, final U result);
}
