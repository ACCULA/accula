package org.accula.api.handler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Lamtev
 */
class SuggesterTest {
    @Test
    void test() {
        final var suggester = new Suggester();
        final var orig = new ArrayList<>(List.of(
            "2019-highload-dht",
            "2020-ads",
            "2018-highload-kv",
            "2019-db-lsm",
            "noise-string",
            "2020-highload-dht",
            "2020-db-lsm",
            "2017-highload-kv",
            "noise-string-2",
            "2018-db-kv"
        ));
        Collections.shuffle(orig);

        final var highloadRes = suggester.suggest("2021-highload-dht", orig, Function.identity());
        final var highloadExpected = orig.stream()
            .filter(s -> s.contains("highload"))
            .collect(Collectors.toSet());
        final var highloadActual = highloadRes.stream()
            .limit(4)
            .collect(Collectors.toSet());
        assertEquals(highloadExpected, highloadActual);

        final var dbRes = suggester.suggest("2021-db-lsm", orig, Function.identity());
        final var dbExpected = orig.stream()
            .filter(s -> s.contains("db"))
            .collect(Collectors.toSet());
        final var dbActual = dbRes.stream()
            .limit(3)
            .collect(Collectors.toSet());
        assertEquals(dbExpected, dbActual);
    }
}