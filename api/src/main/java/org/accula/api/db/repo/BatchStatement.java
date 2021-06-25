package org.accula.api.db.repo;

import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
final class BatchStatement {
    private static final String COLLECTION_MARKER = "($collection)";
    private static final char LEFT_PARENTHESIS = '(';
    private static final char RIGHT_PARENTHESIS = ')';
    private static final char COMMA = ',';
    private static final char SINGLE_QUOTATION_MARK = '\'';
    private static final String SINGLE_QUOTATION_MARK_STRING = "'";
    private static final String ESCAPED_SINGLE_QUOTATION_MARK_STRING = "''";
    private static final Object NULL = null;
    private final Connection connection;
    private final String sql;
    private final int indexOfCollectionMarker;
    @Nullable
    private Supplier<StringBuilder> boundValuesProducer;

    static BatchStatement of(final Connection connection, @Language("SQL") final String sql) {
        return new BatchStatement(connection, sql);
    }

    private BatchStatement(final Connection connection, final String sql) {
        this.connection = connection;
        this.sql = sql;
        this.indexOfCollectionMarker = sql.indexOf(COLLECTION_MARKER);
        if (this.indexOfCollectionMarker == -1) {
            throw new IllegalArgumentException("Sql provided does not contain collection marker " + COLLECTION_MARKER);
        }
    }

    <T> void bind(final Iterator<T> items, final Function<T, Object[]> bind) {
        if (!items.hasNext()) {
            throw new IllegalArgumentException("Items must not be empty");
        }
        final var oldBoundValuesProducer = boundValuesProducer;
        final var isFirstBinding = oldBoundValuesProducer == null;
        boundValuesProducer = () -> {
            final var boundValues = isFirstBinding ? new StringBuilder() : oldBoundValuesProducer.get();
            if (!isFirstBinding) {
                boundValues.append(COMMA);
            }
            while (items.hasNext()) {
                final var bindings = bind.apply(items.next());
                final var bindingsLength = bindings.length;
                if (bindingsLength == 0) {
                    throw new IllegalArgumentException("bind must produce non-empty bindings");
                }
                boundValues.append(LEFT_PARENTHESIS);
                for (int i = 0; i < bindingsLength; ++i) {
                    addBinding(boundValues, bindings[i]);
                    if (i != bindingsLength - 1) {
                        boundValues.append(COMMA);
                    }
                }
                boundValues.append(RIGHT_PARENTHESIS);
                if (items.hasNext()) {
                    boundValues.append(COMMA);
                }
            }

            return boundValues;
        };
    }

    <T> void bind(final Iterable<T> items, final Function<T, Object[]> bind) {
        bind(items.iterator(), bind);
    }

    <T> void bind(final Stream<T> items, final Function<T, Object[]> bind) {
        bind(items.iterator(), bind);
    }

    Flux<PostgresqlResult> execute() {
        if (boundValuesProducer == null) {
            throw new IllegalStateException("Batch statement does not have any bindings");
        }
        return Flux.defer(() -> {
            final var boundSql = sql.substring(0, indexOfCollectionMarker)
                    + boundValuesProducer.get().toString()
                    + sql.substring(indexOfCollectionMarker + COLLECTION_MARKER.length());
            return ((PostgresqlStatement) connection.createStatement(boundSql)).execute();
        });
    }

    private static void addBinding(final StringBuilder sb, @Nullable final Object binding) {
        if (binding == null) {
            sb.append(NULL);
        } else if (binding instanceof String s) {
            sb.append(SINGLE_QUOTATION_MARK);
            sb.append(s.replace(SINGLE_QUOTATION_MARK_STRING, ESCAPED_SINGLE_QUOTATION_MARK_STRING));
            sb.append(SINGLE_QUOTATION_MARK);
        } else if (binding instanceof Instant) {
            sb.append(SINGLE_QUOTATION_MARK);
            sb.append(binding);
            sb.append(SINGLE_QUOTATION_MARK);
        } else if (isInteger(binding) || binding instanceof Boolean) {
            sb.append(binding);
        } else {
            throw new IllegalArgumentException("Not yet supported class: " + binding.getClass().getName());
        }
    }

    private static boolean isInteger(final Object o) {
        return o instanceof Long || o instanceof Integer || o instanceof Byte || o instanceof Short;
    }
}
