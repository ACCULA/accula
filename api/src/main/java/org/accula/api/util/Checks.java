package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
public final class Checks {
    private Checks() {
    }

    public static <T> T notNull(@Nullable final T aNullable, final String name) {
        if (aNullable == null) {
            throw new NullPointerException(name + " MUST NOT be null");
        }
        return aNullable;
    }
}
