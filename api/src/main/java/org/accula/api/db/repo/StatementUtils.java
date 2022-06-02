package org.accula.api.db.repo;

import io.r2dbc.spi.Statement;

import java.util.function.BiConsumer;

/**
 * @author Anton Lamtev
 */
final class StatementUtils {
    private StatementUtils() {
    }

    static <S extends Statement, T> S bindIterable(final Iterable<T> iterable, final S statement, final BiConsumer<T, S> bind) {
        final var iterator = iterable.iterator();
        while (iterator.hasNext()) {
            bind.accept(iterator.next(), statement);
            if (iterator.hasNext()) {
                statement.add();
            }
        }
        return statement;
    }
}
