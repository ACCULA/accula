package org.accula.api.db.repo;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public interface ConnectionProvidedRepo {
    ConnectionProvider connectionProvider();

    default <T> Mono<T> withConnection(final Function<? super Connection, ? extends Mono<T>> connectionUse) {
        return Mono.usingWhen(
                connectionProvider().get(),
                connectionUse,
                Connection::close,
                this::handleError,
                Connection::close
        );
    }

    default <T> Flux<T> manyWithConnection(final Function<? super Connection, ? extends Flux<T>> connectionUse) {
        return Flux.usingWhen(
                connectionProvider().get(),
                connectionUse,
                Connection::close,
                this::handleError,
                Connection::close
        );
    }

    default <T> Mono<T> transactional(final Function<? super Connection, ? extends Mono<T>> connectionUse) {
        return Mono.usingWhen(
            connectionProvider().get(),
            connection -> Mono.fromDirect(connection.beginTransaction()).then(connectionUse.apply(connection)),
            ConnectionProvidedRepo::commitAndClose,
            this::handleTransactionError,
            ConnectionProvidedRepo::rollbackAndClose
        );
    }

    default <T> Flux<T> transactionalMany(final Function<? super Connection, ? extends Flux<T>> connectionUse) {
        return Flux.usingWhen(
            connectionProvider().get(),
            connection -> Mono.fromDirect(connection.beginTransaction()).thenMany(connectionUse.apply(connection)),
            ConnectionProvidedRepo::commitAndClose,
            this::handleTransactionError,
            ConnectionProvidedRepo::commitAndClose
        );
    }

    static <T> Mono<T> column(final Result result, final String name, final Class<T> clazz) {
        return Mono.from(result
                .map((row, metadata) -> Objects.requireNonNull(
                        row.get(name, clazz),
                        () -> "Row MUST contain column named %s of class %s".formatted(name, clazz.getCanonicalName())
                )));
    }

    static <T> Flux<T> columnFlux(final Result result, final String name, final Class<T> clazz) {
        return Flux.from(result
                .map((row, metadata) -> Objects.requireNonNull(
                        row.get(name, clazz),
                        () -> "Row MUST contain column named %s of class %s".formatted(name, clazz.getCanonicalName())
                )));
    }

    static <T> Mono<T> convert(final Result result, final Function<Row, T> transform) {
        return Mono.from(result.map((row, metadata) -> transform.apply(row)));
    }

    static <T> Flux<T> convertMany(final Result result, final Function<Row, T> transform) {
        return Flux.from(result.map((row, metadata) -> transform.apply(row)));
    }

    private Publisher<Void> handleError(final Connection connection, final Throwable t) {
        LoggerFactory.getLogger(getClass()).error("Error during request", t);
        return connection.close();
    }

    private Publisher<Void> handleTransactionError(final Connection connection, final Throwable t) {
        log().error("Error during transaction", t);
        return rollbackAndClose(connection);
    }

    private Logger log() {
        return LoggerFactory.getLogger(getClass());
    }

    private static Publisher<Void> commitAndClose(final Connection connection) {
        return Mono.fromDirect(connection.commitTransaction())
            .thenEmpty(connection.close());
    }

    private static Publisher<Void> rollbackAndClose(final Connection connection) {
        return Mono.fromDirect(connection.rollbackTransaction())
            .thenEmpty(connection.close());
    }

    interface ConnectionProvider extends Supplier<Mono<Connection>> {
        @Override
        Mono<Connection> get();
    }
}
