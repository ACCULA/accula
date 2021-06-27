package org.accula.api.token.java;

import org.accula.api.code.FileEntity;
import org.accula.api.code.lines.LineSet;
import org.accula.api.token.LanguageTokenProvider;
import org.accula.api.token.BaseLanguageTokenProviderTest;
import org.junit.jupiter.api.Test;

/**
 * @author Anton Lamtev
 */
public class JavaTokenProviderTest extends BaseLanguageTokenProviderTest {
    public static final String content1 = """
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
    public static final String content2 = """
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
    public static final String content3 = """
        public class Cell {
            public Cell(@Another ByteBuffer k, final Value v) {
            }
            public ByteBuffer k() {
            }
            public Value v() {
            }
        }
        """;
    public static final FileEntity<String> jf1 = new FileEntity<>("1", "Cell.java", content1, LineSet.all());
    public static final FileEntity<String> jf2 = new FileEntity<>("2", "Cell.java", content2, LineSet.all());
    public static final FileEntity<String> jf3 = new FileEntity<>("3", "Cell.java", content3, LineSet.all());

    private final JavaTokenProvider<String> tokenProvider = new JavaTokenProvider<>();

    @Override
    @SuppressWarnings("unchecked")
    public <Ref> LanguageTokenProvider<Ref> tokenProvider() {
        return (LanguageTokenProvider<Ref>) tokenProvider;
    }

    @Test
    void test() {
        testSupportsFile(jf1);
        testDoesNotSupportFile(new FileEntity<>("", "Cell.cpp", "", LineSet.empty()));

        testMethodCount(jf1, 3);
        testMethodCount(jf2, 3);
        testMethodCount(jf3, 0);
    }
}
