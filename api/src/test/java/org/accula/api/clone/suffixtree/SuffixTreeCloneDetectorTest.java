package org.accula.api.clone.suffixtree;

import org.accula.api.code.FileEntity;
import org.accula.api.code.lines.LineSet;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Lamtev
 */
public class SuffixTreeCloneDetectorTest {
    public static final String F1 = """
            public class Cell {
                public Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
                    this.key = key;
                    this.value = value;
                }
                public Value getValue() {
                    return value;
                }
                public ByteBuffer getKey() {
                    println(k);
                    //comment
                    return key.asReadOnlyBuffer();
                }
            }
            """;
    public static final String F2 = """
            public class Cell {
                public Cell(@Another ByteBuffer k, final Value v) {
                    Objects.requireNonNull(k);
                    Objects.requireNonNull(v);
                    this.k = k;
                    this.v = v;
                }
                public ByteBuffer k() {
                    return k.asReadOnlyBuffer();
                }
                public Value v() {
                    return v;
                }
            }
            """;
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
    final TokenProvider<String> tokenProvider = TokenProvider.of(TokenProvider.Language.JAVA);
    SuffixTreeCloneDetector<Token<String>, String> detector;

    @BeforeEach
    void setUp() {
        detector = new SuffixTreeCloneDetector<>();
    }

    @Test
    void test1() {
        final var f1 = new FileEntity<>("ref1", "Cell.java", F1, LineSet.all());
        final var f2 = new FileEntity<>("ref2", "Cell.java", F2, LineSet.all());

        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(f1, f2))
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
}
