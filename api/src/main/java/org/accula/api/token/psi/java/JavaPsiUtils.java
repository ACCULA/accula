package org.accula.api.token.psi.java;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.java.stubs.JavaAnnotationElementType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.TokenSet;
import org.accula.api.token.psi.PsiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public final class JavaPsiUtils {
    private static final TokenSet TOKENS_TO_EXCLUDE = TokenSet.orSet(
            TokenSet.WHITE_SPACE,
            TokenSet.create(
                    new JavaAnnotationElementType(),
                    JavaTokenType.FINAL_KEYWORD,
                    JavaTokenType.VAR_KEYWORD,
                    JavaTokenType.C_STYLE_COMMENT,
                    JavaTokenType.END_OF_LINE_COMMENT,
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

    private JavaPsiUtils() {
    }

    public static List<PsiMethod> methods(final PsiElement root) {
        final var methods = new ArrayList<PsiMethod>();
        PsiUtils.forEachDescendantOfType(root, PsiMethod.class, method -> {
            if (method.getBody() != null) {
                methods.add(method);
            }
        });
        return methods;
    }

    public static boolean isValuableToken(final PsiElement token) {
        return !TOKENS_TO_EXCLUDE.contains(token.getNode().getElementType());
    }
}
