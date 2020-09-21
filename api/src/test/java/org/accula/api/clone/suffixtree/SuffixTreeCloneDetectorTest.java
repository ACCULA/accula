package org.accula.api.clone.suffixtree;

import org.accula.api.code.FileEntity;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
class SuffixTreeCloneDetectorTest {
    final TokenProvider<String> tokenProvider = TokenProvider.of(TokenProvider.Language.JAVA);
    SuffixTreeCloneDetector<Token<String>, String> detector;

    @BeforeEach
    void setUp() {
        detector = new SuffixTreeCloneDetector<>();
    }

    @Test
    void test1() {
        final var f1 = new FileEntity<>("ref1", "Cell.java", """
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
                """);
        final var f2 = new FileEntity<>("ref2", "Cell.java", """
                public class Cell {
                    public Cell(@Another ByteBuffer k, final Value v) {
                        Objects.requireNonNull(key);
                        Objects.requireNonNull(value);
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
                """);

        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(f1, f2))
                .map(stream -> stream.collect(toList()))
                .collectList())
                .expectNextMatches(methods -> {
                    methods.forEach(method -> detector.addTokens(method));
                    return detector.cloneClassesAfterTransform(Function.identity()).size() == 3;
                })
                .verifyComplete();
    }

    @Test
    void test2() {
        final var f1 = new FileEntity<>("ref1", "SSTable.java", """
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
                 """);
        final var f2 = new FileEntity<>("ref2", "SSTable.java", """
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
                 """);

        StepVerifier.create(tokenProvider.tokensByMethods(Flux.just(f1, f2))
                .map(stream -> stream.collect(toList()))
                .collectList())
                .expectNextMatches(methods -> {
                    methods.forEach(method -> detector.addTokens(method));
                    final var cloneClasses = detector.cloneClassesAfterTransform(Function.identity());
                    //It is not straightforward but there are 5 clone classes here
                    //Actually 4 of them are subclasses of the largest one
                    //Since SuffixTreeCloneDetector do not perform any smart filtering, the result is ok
                    return cloneClasses.size() == 5;
                })
                .verifyComplete();
    }
}
