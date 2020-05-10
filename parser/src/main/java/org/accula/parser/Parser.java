package org.accula.parser;

import reactor.core.publisher.Flux;

import java.io.InputStream;

@FunctionalInterface
public interface Parser<T> {
    Flux<T> getTokens(final InputStream file);
}
