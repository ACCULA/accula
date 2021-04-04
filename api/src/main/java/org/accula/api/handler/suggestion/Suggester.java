package org.accula.api.handler.suggestion;

import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Anton Lamtev
 */
public final class Suggester {
    private final StringDistance stringDistance = new NGram();

    public <T> List<T> suggest(final String reference, final Collection<T> variants, final Function<T, String> toString) {
        return variants
            .stream()
            .sorted(Comparator.comparing(toString.andThen(variant -> stringDistance.distance(reference, variant))))
            .collect(Collectors.toList());
    }
}
