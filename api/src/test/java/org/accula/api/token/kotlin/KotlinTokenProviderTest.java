package org.accula.api.token.kotlin;

import org.accula.api.code.FileEntity;
import org.accula.api.code.lines.LineSet;
import org.accula.api.token.BaseLanguageTokenProviderTest;
import org.accula.api.token.LanguageTokenProvider;
import org.junit.jupiter.api.Test;

/**
 * @author Anton Lamtev
 */
public class KotlinTokenProviderTest extends BaseLanguageTokenProviderTest {
    private static final String content1 = """
        package org.accula.api.token

        import java.nio.ByteBuffer

        class Cell(val key: ByteBuffer, val value: ByteBuffer) {
            fun doSomeComplexLogic(aParam: String): Map<Int, Long> {
                while (this.key.isDirect) {
                    if (value.isReadOnly) {
                        break
                    }
                }

                val x = this
                val y: Cell = this
                val z: Int = 1
                val w: ByteBuffer = this.key

                return mapOf()
            }
        }

        fun Cell.ext(int: Int) {
            print(int)
        }

        fun justAFun() {
            for (i in 0..10) {
                print(i)
            }
        }

        fun `endlessOrNot? 🙈`(n: Int): Int {
            if (n == 0) return 0;
               
            for (i in 1..n) {
                return `endlessOrNot? 🙈`(i - 1)
            }
               
            return 0;
        }
         """;
    public static final FileEntity<String> kf1 = new FileEntity<>("1", "Cell.kt", content1, LineSet.all());

    private final KotlinTokenProvider<String> tokenProvider = new KotlinTokenProvider<>();

    @Override
    @SuppressWarnings("unchecked")
    public <Ref> LanguageTokenProvider<Ref> tokenProvider() {
        return (LanguageTokenProvider<Ref>) tokenProvider;
    }

    @Test
    void test() {
        testSupportsFile(kf1);
        testDoesNotSupportFile(new FileEntity<>("", "Cell.ts", "", LineSet.empty()));

        testMethodCount(kf1, 4);
    }
}
