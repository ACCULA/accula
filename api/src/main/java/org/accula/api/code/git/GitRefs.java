package org.accula.api.code.git;

/**
 * @author Anton Lamtev
 */
public final class GitRefs {
    private static final String ORIGIN_HEAD = "origin/HEAD";

    private GitRefs() {
    }

    public static String originHead() {
        return ORIGIN_HEAD;
    }

    public static String inclusive(final String ref) {
        if (ref.endsWith("^")) {
            return ref;
        }
        return ref + "^";
    }

    public static String origin(final String ref) {
        if (ref.contains("origin/")) {
            return ref;
        }
        return "origin/" + ref;
    }
}
