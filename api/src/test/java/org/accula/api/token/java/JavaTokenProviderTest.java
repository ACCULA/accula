package org.accula.api.token.java;

import org.accula.api.clone.suffixtree.SuffixTreeCloneDetectorTest;
import org.accula.api.code.FileEntity;
import org.accula.api.token.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
class JavaTokenProviderTest {
    TokenProvider<String> tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = TokenProvider.of(TokenProvider.Language.JAVA);
    }

    @Test
    void test() {
        final var f1 = new FileEntity<>("ref1", "Cell.java", SuffixTreeCloneDetectorTest.F1);
        final var f2 = new FileEntity<>("ref2", "Cell.java", SuffixTreeCloneDetectorTest.F2);
        final var f3 = new FileEntity<>("ref3", "Cell.java", """
                public class Cell {
                    public Cell(@Another ByteBuffer k, final Value v) {
                    }
                    public ByteBuffer k() {
                    }
                    public Value v() {
                    }
                }
                """);

        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(f1, f2, f3))
                .collectList())
                .expectNextMatches(methods -> {
                    if (methods.size() != 9) {
                        return false;
                    }
                    return methods.stream()
                            .filter(method ->
                                    method.stream()
                                            .allMatch(token -> token.ref().equals("ref3")))
                            .allMatch(Collection::isEmpty);
                })
                .verifyComplete();
    }
}
