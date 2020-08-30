package org.accula.api.psi;

import org.accula.api.util.Lambda;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public final class TraverseUtils {
    private TraverseUtils() {
    }

    public static <T> Stream<T> dfs(final T root, final Function<T, Stream<T>> children) {
        return Stream.concat(
                Stream.of(root),
                children.apply(root)
                        .flatMap(Lambda.passingTailArg(TraverseUtils::dfs, children)));
    }

    public static <T> Function<T, Stream<T>> stream(final Function<T, T[]> array) {
        return t -> Arrays.stream(array.apply(t));
    }

    public static <T> Function<T, Stream<T>> stream(final Function<T, T[]> array, final Predicate<T> filter) {
        return t -> Arrays
                .stream(array.apply(t))
                .filter(filter);
    }
}
