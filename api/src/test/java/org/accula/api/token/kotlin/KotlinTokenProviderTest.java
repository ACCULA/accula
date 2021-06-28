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
    public static final String content1 = """
        package org.accula.api.token

        import java.nio.ByteBuffer

        class Cell(val key: ByteBuffer, val value: ByteBuffer) {
            fun doSomeComplexLogic(aParam: String): Map<Int, Long> {
                while (this.key.isDirect) {
                    if (value.isReadOnly) {
                        break
                    }
                }

                val aa: String = "12"
                var a = "2"
                val x = this
                val y: Cell = this
                val z: Int = 1
                functionCall(false, 1, null, '', y)

                val aBoolean = false

                var u: Char = '0'
                val f = 1.28

                val w: ByteBuffer = this.key
                val ww = key
                var zz: String? = null

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
            if (n == 0) return 0

            for (i in 1..n) {
                return `endlessOrNot? 🙈`(i - 1)
            }

            return 0;
        }
         """;
    public static final String content2 = """
        class NotACell(val aKey: ByteArray, val aValue: ByteArray) {
            fun function(aParam: String, anUnusedParam: Int): Set<Long> {
                while (this.aKey.isNotEmpty) {
                    if (aValue.isNotEmpty) {
                        break
                    }
                }
                val z: Int = 100
                val f = 3.14
                val x = this
                var y: NotACell = this
                val bb: String = "1"

                methodCall(true, null, 1, "", '')

                val aBoolean = true
                var u: Char = '5'
                val b = ""
                var w: ByteArray = this.aKey
                val ww = aKey
                var zz: String? = "null"
                return setOf()
            }
        }
         """;
    public static final FileEntity<String> kf1 = new FileEntity<>("1", "Cell.kt", content1, LineSet.all());
    public static final FileEntity<String> kf2 = new FileEntity<>("1", "NotACell.kt", content2, LineSet.all());

    private final KotlinTokenProvider<Object> tokenProvider = new KotlinTokenProvider<>();

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

        final var f1 = new FileEntity<>("1", "Cell.kt", content1, LineSet.inRange(1, 33));
        final var m1 = methods(f1).findFirst().orElseThrow(AssertionError::new);
        final var m2 = methods(kf2).findFirst().orElseThrow(AssertionError::new);
        testEqualMethods(m1, m2);
    }
}
