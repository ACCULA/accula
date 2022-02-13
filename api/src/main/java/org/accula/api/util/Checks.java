package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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

    public static <T> T notNull(@Nullable final T aNullable, final Supplier<String> info) {
        if (aNullable == null) {
            throw new NullPointerException(info.get() + " MUST NOT be null");
        }
        return aNullable;
    }
}
