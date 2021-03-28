package org.accula.api.util;

import lombok.Getter;
import lombok.Value;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.accula.api.util.ComparatorsTest.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Lamtev
 */
class ComparatorsTest {
    @Getter
    final List<$> l = List.of(
            $("c", Integer.MAX_VALUE),
            $("c", Integer.MIN_VALUE),
            $("abc", 5),
            $("abc", Integer.MAX_VALUE),
            $("def", 5),
            $("abc", Integer.MIN_VALUE),
            $("b", 0),
            $("abc", 10)
    );

    @RepeatedTest(5)
    void testMinBy() {
        final var l = Stream.generate(this::l)
                .flatMap(List::stream)
                .limit(1_000_000L)
                .collect(Collectors.toList());
        Collections.shuffle(l);
        assertEquals($("abc", Integer.MIN_VALUE), l
                .stream()
                .reduce(Comparators.minBy($::s, $::i))
                .orElseThrow(IllegalStateException::new));
    }

    @Value
    static class $ implements Comparable<$> {
        String s;
        int i;

        @Override
        public int compareTo($ o) {
            return s.compareTo(o.s);
        }

        static $ $(String s, int i) {
            return new $(s, i);
        }
    }
}
