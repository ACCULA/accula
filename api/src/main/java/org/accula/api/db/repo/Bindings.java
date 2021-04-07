package org.accula.api.db.repo;

/**
 * @author Anton Lamtev
 */
public final class Bindings {
    private Bindings() {
    }

    public static Object[] of(final Object... row) {
        return row;
    }
}
