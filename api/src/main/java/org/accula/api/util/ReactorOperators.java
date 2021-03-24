package org.accula.api.util;

import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Anton Lamtev
 */
public final class ReactorOperators {
    private ReactorOperators() {
    }

    public static <Next> Consumer<Signal<Next>> onNextWithContext(final BiConsumer<Next, ContextView> nextAndContextConsumer) {
        return signal -> {
            if (!signal.isOnNext()) {
                return;
            }

            nextAndContextConsumer.accept(Objects.requireNonNull(signal.get()), signal.getContextView());
        };
    }

    public static <T> Consumer<Signal<T>> doFinally(final Runnable doFinally) {
        return signal -> {
            if (!signal.isOnComplete() && !signal.isOnError()) {
                return;
            }
            doFinally.run();
        };
    }
}
