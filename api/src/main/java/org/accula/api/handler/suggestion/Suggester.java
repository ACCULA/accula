package org.accula.api.handler.suggestion;

import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public final class Suggester {
    private final StringDistance stringDistance = new NGram();

    public <T> Stream<T> suggest(final String reference, final Collection<T> variants, final Function<T, String> toString) {
        return variants
            .stream()
            .sorted(Comparator.comparing(toString.andThen(variant -> stringDistance.distance(reference, variant))));
    }
}
