package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Anton Lamtev
 */
public final class Checks {
    private Checks() {
    }

    public static <T> T notNull(@Nullable final T aNullable, final String name) {
        return Objects.requireNonNull(aNullable, name + " MUST NOT be null");
    }
}
