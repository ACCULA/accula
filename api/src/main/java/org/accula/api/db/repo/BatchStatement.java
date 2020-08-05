package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
final class BatchStatement {
    private static final String COLLECTION_MARKER = "($collection)";
    private static final String NULL = "NULL";
    private final Connection connection;
    private final String sql;
    private final int indexOfCollectionMarker;
    @Nullable
    private Supplier<String> boundSqlProducer;

    static BatchStatement of(final Connection connection, @Language("SQL") final String sql) {
        return new BatchStatement(connection, sql);
    }

    private BatchStatement(final Connection connection, final String sql) {
        this.connection = connection;
        this.sql = sql;
        this.indexOfCollectionMarker = sql.indexOf(COLLECTION_MARKER);
        if (this.indexOfCollectionMarker == -1) {
            throw new IllegalArgumentException("sql provided does not contain collection marker ($collection)");
        }
    }

    <T> void bind(final Collection<T> collection, final Function<T, Object[]> bind) {
        boundSqlProducer = () -> {
            final StringBuilder rowsWithBindings = new StringBuilder();
            for (final var el : collection) {
                final var bindings = bind.apply(el);
                final var bindingsLength = bindings.length;
                if (bindingsLength == 0) {
                    throw new IllegalStateException("Bindings must be present");
                }
                rowsWithBindings.append('(');
                for (int i = 0; i < bindingsLength; ++i) {
                    final var binding = bindings[i];
                    if (binding == null) {
                        rowsWithBindings.append(NULL);
                    } else if (binding instanceof String || binding instanceof Instant) {
                        rowsWithBindings.append('\'');
                        rowsWithBindings.append(binding);
                        rowsWithBindings.append('\'');
                    } else if (isInteger(binding) || binding instanceof Boolean) {
                        rowsWithBindings.append(binding);
                    } else {
                        throw new IllegalStateException("Not yet supported class: " + binding.getClass().getName());
                    }
                    if (i != bindingsLength - 1) {
                        rowsWithBindings.append(',');
                    }
                }
                rowsWithBindings.append("),");
            }
            if (rowsWithBindings.charAt(rowsWithBindings.length() - 1) == ',') {
                rowsWithBindings.deleteCharAt(rowsWithBindings.length() - 1);
            }

            return sql.substring(0, indexOfCollectionMarker)
                   + rowsWithBindings.toString()
                   + sql.substring(indexOfCollectionMarker + COLLECTION_MARKER.length());
        };
    }

    Flux<PostgresqlResult> execute() {
        if (boundSqlProducer == null) {
            throw new IllegalStateException("Batch statement does not have any bindings");
        }
        return Flux.defer(() -> ((PostgresqlStatement) connection.createStatement(boundSqlProducer.get())).execute());
    }

    private static boolean isInteger(final Object o) {
        return o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long;
    }
}
