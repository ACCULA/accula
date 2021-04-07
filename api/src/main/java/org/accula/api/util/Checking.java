package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Anton Lamtev
 */
public final class Checking {
    private Checking() {
    }

    public static <T> T notNull(@Nullable final T aNullable, final String name) {
        return Objects.requireNonNull(aNullable, () -> name + " MUST NOT be null");
    }

    public static <T> T present(@Nullable final T mightBeAbsent, final String name) {
        return Objects.requireNonNull(mightBeAbsent, () -> name + "MUST be present");
    }
}
