package org.accula.api.util;

import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
public final class Strings {
    // https://stackoverflow.com/a/9584469
    private static final String SPACES_EXCEPT_BETWEEN_QUOTES =
        "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";

    private Strings() {
    }

    @Nullable
    public static String suffixAfterPrefix(final String string, final String prefix) {
        if (!string.startsWith(prefix)) {
            return null;
        }

        return string.substring(prefix.length());
    }

    public static boolean startsWithSha40(final String string) {
        if (string.length() < 40) {
            return false;
        }
        for (int i = 0; i < 40; ++i) {
            final var ch = string.charAt(i);
            if (!('0' <= ch && ch <= '9') && !('a' <= ch && ch <= 'f')) {
                return false;
            }
        }
        return true;
    }

    public static String[] splitBySpaceIgnoringSpacesBetweenQuotes(final String string) {
        return string.split(SPACES_EXCEPT_BETWEEN_QUOTES);
    }
}
