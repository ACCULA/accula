package org.accula.api.clone.suffixtree;

import org.accula.api.code.FileEntity;
import org.accula.api.code.lines.LineSet;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.token.java.JavaTokenProvider;
import org.accula.api.token.kotlin.KotlinTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.accula.api.token.java.JavaTokenProviderTest.jf1;
import static org.accula.api.token.java.JavaTokenProviderTest.jf2;
import static org.accula.api.token.kotlin.KotlinTokenProviderTest.content1;
import static org.accula.api.token.kotlin.KotlinTokenProviderTest.content2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Anton Lamtev
 */
public class SuffixTreeCloneDetectorTest {
    public static final String F3 = """
            public class SSTable {
                private int getElementPosition(final ByteBuffer key) throws IOException {
                    int left = 0;

                    int right = amountOfElements - 1;
                    while (left <= right) {
                        final int mid = (left + right) / 2;
                        final ByteBuffer midKey = getKey(mid);
                        
                        final int compareResult = midKey.compareTo(key);
                        if (compareResult < 0) {
                            left = mid + 1;
                        } else if(compareResult > 0) {
                            right = mid - 1;
                        } else {
                            return mid;
                        }
                    }
                    return left;
                }
            }
             """;
    public static final String F4 = """
            public class SSTable {
                private int getPosition(final ByteBuffer key) throws IOException {
                    int left = 0;
                    int right = amountOfElements - 1;
                    while (left <= right) {
                        final int mid = (left + right) / 2;
                        final ByteBuffer midValue = getKey(mid);
                        final int cmp = midValue.compareTo(key);
                        //left
                        if (cmp < 0) {
                            left = mid + 1;
                        //right
                        } else if (cmp > 0) {
                            right = mid - 1;
                        //mid
                        } else {
                            return mid;
                        }
                    }
                    return left;
                }
            }
             """;
    final TokenProvider<String> tokenProvider = new TokenProvider<>(List.of(
        new JavaTokenProvider<>(),
        new KotlinTokenProvider<>()
    ));
    SuffixTreeCloneDetector<Token<String>, String> detector;

    @BeforeEach
    void setUp() {
        detector = new SuffixTreeCloneDetector<>("");
    }

    @Test
    void test1() {
        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(jf1, jf2))
                .collectList())
                .expectNextMatches(methods -> {
                    methods.forEach(method -> detector.addTokens(method));
                    assertEquals(5, detector.cloneClasses(cloneClass -> true).size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void test2() {
        final var f1 = new FileEntity<>("ref1", "SSTable.java", F3, LineSet.all());
        final var f2 = new FileEntity<>("ref2", "SSTable.java", F4, LineSet.all());

        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(f1, f2))
                .collectList())
                .expectNextMatches(methods -> {
                    methods.forEach(method -> detector.addTokens(method));
                    final var cloneClasses = detector.cloneClasses(it -> true);
                    return cloneClasses.size() == 1 && cloneClasses.get(0).cloneCount() == 2;
                })
                .verifyComplete();
    }

    @Test
    void test3() {
        final var f1 = new FileEntity<>("1", "Cell.kt", content1, LineSet.inRange(1, 33));
        final var f2 = new FileEntity<>("2", "myFile.kt", content2, LineSet.all());
        final var methods = tokenProvider
            .tokensByMethods(Flux.just(f1, f2))
            .collectList()
            .block();
        assertNotNull(methods);

        for (var m : methods) {
            detector.addTokens(m);
        }
        final var cloneClasses = detector.cloneClasses(cc -> cc.length() > 10);
        assertEquals(1, cloneClasses.size());
        assertEquals(2, cloneClasses.get(0).cloneCount());
    }
}
