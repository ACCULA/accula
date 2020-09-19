package org.accula.api.detector.psi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.impl.java.stubs.JavaAnnotationElementType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.TokenSet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public final class PsiUtils {
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

    private PsiUtils() {
    }

    public static <T extends PsiElement> void forEachDescendantOfType(final PsiElement root,
                                                                      final Class<T> type,
                                                                      final Consumer<? super T> onEach) {
        root.accept(new PsiRecursiveElementVisitor() {
            @SuppressWarnings("unchecked")
            @Override
            public void visitElement(final PsiElement element) {
                super.visitElement(element);
                if (type.isAssignableFrom(element.getClass())) {
                    onEach.accept((T) element);
                }
            }
        });
    }

    public static List<PsiElement> methodBodies(final PsiElement root) {
        final var methodBodies = new ArrayList<PsiElement>();
        forEachDescendantOfType(root, PsiMethod.class, method -> {
            final var body = method.getBody();
            if (body != null) {
                methodBodies.add(body);
            }
        });
        return methodBodies;
    }

    public static boolean isValuableToken(final PsiElement token) {
        return !TOKENS_TO_EXCLUDE.contains(token.getNode().getElementType());
    }

    public static Stream<PsiElement> parentsWithSelf(final PsiElement self) {
        if (self instanceof PsiFile) {
            return Stream.empty();
        }
        final var parent = self.getParent();
        return Stream.concat(
                Stream.of(parent),
                parentsWithSelf(parent));
    }
}
