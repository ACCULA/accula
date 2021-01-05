package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
public final class Strings {
    private static final String EMPTY = "";

    private Strings() {
    }

    @Nullable
    public static String suffixAfterPrefix(final String string, final String prefix) {
        final var prefixLength = prefix.length();

        if (prefixLength >= string.length()) {
            return null;
        }

        for (int i = 0; i < prefixLength; ++i) {
            if (prefix.charAt(i) != string.charAt(i)) {
                return null;
            }
        }

        return string.substring(prefixLength);
    }

    public static String empty() {
        return EMPTY;
    }
}
