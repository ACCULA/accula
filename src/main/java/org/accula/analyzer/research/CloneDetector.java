package org.accula.analyzer.research;

import reactor.core.publisher.Flux;

@FunctionalInterface
public interface CloneDetector<T> {
    Flux<CloneInfo> findClones(final Flux<T> files);
}
