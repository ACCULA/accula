package org.accula.analyzer;

import reactor.core.publisher.Flux;

@FunctionalInterface
public interface Analyzer <T, U> {
    Flux<? extends U> analyze(final Flux<T> data, final float threshold, final int minLength);
}
