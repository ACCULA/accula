package org.accula.api.token.psi.java;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.java.stubs.JavaAnnotationElementType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.accula.api.code.lines.LineRange;
import org.accula.api.token.psi.PsiUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Anton Lamtev
 */
public final class JavaPsiUtils {
    private static final TokenSet OPTIONAL_TOKENS = TokenSet.create(
            new JavaAnnotationElementType(),
            JavaTokenType.FINAL_KEYWORD,
            JavaTokenType.C_STYLE_COMMENT,
            JavaTokenType.END_OF_LINE_COMMENT,
            JavaTokenType.SYNCHRONIZED_KEYWORD,
            JavaTokenType.THIS_KEYWORD
    );
    private static final TokenSet TOKENS_TO_EXCLUDE = TokenSet.orSet(
            TokenSet.WHITE_SPACE,
            OPTIONAL_TOKENS,
            TokenSet.create(
                    JavaTokenType.LBRACE,
                    JavaTokenType.RBRACE,
                    JavaTokenType.SEMICOLON,
                    JavaTokenType.COLON,
                    JavaTokenType.COMMA,
                    JavaTokenType.LPARENTH,
                    JavaTokenType.RPARENTH,
                    JavaElementType.CODE_BLOCK
            )
    );
    private static final TokenSet LITERALS = TokenSet.create(
            JavaTokenType.TRUE_KEYWORD,
            JavaTokenType.FALSE_KEYWORD,
            JavaTokenType.CHARACTER_LITERAL,
            JavaTokenType.INTEGER_LITERAL,
            JavaTokenType.LONG_LITERAL,
            JavaTokenType.FLOAT_LITERAL,
            JavaTokenType.DOUBLE_LITERAL,
            JavaTokenType.STRING_LITERAL,
            JavaTokenType.TEXT_BLOCK_LITERAL,
            JavaTokenType.NULL_KEYWORD
    );
    private static final TokenSet TYPE_KEYWORDS_AND_REFS = TokenSet.create(
            JavaTokenType.VAR_KEYWORD,
            JavaTokenType.BOOLEAN_KEYWORD,
            JavaTokenType.BYTE_KEYWORD,
            JavaTokenType.CHAR_KEYWORD,
            JavaTokenType.SHORT_KEYWORD,
            JavaTokenType.INT_KEYWORD,
            JavaTokenType.LONG_KEYWORD,
            JavaTokenType.FLOAT_KEYWORD,
            JavaTokenType.DOUBLE_KEYWORD,
            JavaElementType.JAVA_CODE_REFERENCE
    );

    private JavaPsiUtils() {
    }

    public static List<PsiMethod> methods(final PsiElement root) {
        return methods(root, null);
    }

    public static List<PsiMethod> methods(final PsiElement root, @Nullable final Predicate<LineRange> lineRangeFilter) {
        final var methods = new ArrayList<PsiMethod>();
        PsiUtils.forEachDescendantOfType(root, PsiMethod.class, method -> {
            if (method.getBody() != null) {
                if (lineRangeFilter == null || lineRangeFilter.test(PsiUtils.lineRange(method))) {
                    methods.add(method);
                }
            }
        });
        return methods;
    }

    public static boolean isValuableToken(final PsiElement token) {
        return !TOKENS_TO_EXCLUDE.contains(token.getNode().getElementType());
    }

    public static String optimizeTokenString(final IElementType token) {
        final var tokenString = token.toString();
        if (LITERALS.contains(token)) {
            return "_LITERAL";
        }
        if (TYPE_KEYWORDS_AND_REFS.contains(token)) {
            return "_TYPE";
        }
        return tokenString;
    }
}
