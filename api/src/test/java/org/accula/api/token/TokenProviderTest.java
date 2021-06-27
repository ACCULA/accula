package org.accula.api.token;

import org.accula.api.token.java.JavaTokenProvider;
import org.accula.api.token.kotlin.KotlinTokenProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.accula.api.token.java.JavaTokenProviderTest.jf1;
import static org.accula.api.token.java.JavaTokenProviderTest.jf2;
import static org.accula.api.token.java.JavaTokenProviderTest.jf3;
import static org.accula.api.token.kotlin.KotlinTokenProviderTest.kf1;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anton Lamtev
 */
class TokenProviderTest {
    final TokenProvider<String> tokenProvider = new TokenProvider<>(List.of(
        new JavaTokenProvider<>(),
        new KotlinTokenProvider<>()
    ));
    final TokenProvider<String> javaTokenProvider = new TokenProvider<>(List.of(new JavaTokenProvider<>()));

    @Test
    void testFailure() {
        assertThrows(IllegalArgumentException.class, () -> new TokenProvider<>(List.of()));
    }

    @Test
    void test() {
        tokenProvider
            .tokensByMethods(Flux.just(jf1, jf2, jf3, kf1))
            .collectList()
            .as(StepVerifier::create)
            .expectNextMatches(methods -> methods.size() == 10) // Non-empty methods
            .verifyComplete();

        javaTokenProvider
            .tokensByMethods(Flux.just(kf1))
            .collectList()
            .as(StepVerifier::create)
            .expectNextMatches(List::isEmpty)
            .verifyComplete();
    }
}
