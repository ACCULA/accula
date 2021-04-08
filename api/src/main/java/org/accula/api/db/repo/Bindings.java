package org.accula.api.db.repo;

/**
 * @author Anton Lamtev
 */
final class Bindings {
    private Bindings() {
    }

    static Object[] of(final Object... row) {
        return row;
    }
}
