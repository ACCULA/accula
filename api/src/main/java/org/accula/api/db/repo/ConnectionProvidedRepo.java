package org.accula.api.db.repo;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public interface ConnectionProvidedRepo {
    ConnectionProvider getConnectionProvider();

    default <T> Mono<T> withConnection(final Function<? super Connection, ? extends Mono<T>> connectionUse) {
        return Mono.usingWhen(
                getConnectionProvider().get(),
                connectionUse,
                Connection::close
        );
    }

    default <T> Flux<T> manyWithConnection(final Function<? super Connection, ? extends Flux<T>> connectionUse) {
        return Flux.usingWhen(
                getConnectionProvider().get(),
                connectionUse,
                Connection::close
        );
    }

    static <T> Mono<T> column(final Result result, final String name, final Class<T> clazz) {
        return Mono.from(result
                .map((row, metadata) -> Objects.requireNonNull(row.get(name, clazz))));
    }

    static <T> Flux<T> columnFlux(final Result result, final String name, final Class<T> clazz) {
        return Flux.from(result
                .map((row, metadata) -> Objects.requireNonNull(row.get(name, clazz))));
    }

    static <T> Mono<T> convert(final Result result, final Function<Row, T> transform) {
        return Mono.from(result.map((row, metadata) -> transform.apply(row)));
    }

    static <T> Flux<T> convertMany(final Result result, final Function<Row, T> transform) {
        return Flux.from(result.map((row, metadata) -> transform.apply(row)));
    }

    interface ConnectionProvider extends Supplier<Mono<Connection>> {
        @Override
        Mono<Connection> get();
    }
}
