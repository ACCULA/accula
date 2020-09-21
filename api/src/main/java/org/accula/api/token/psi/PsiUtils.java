package org.accula.api.token.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;

import java.util.function.Consumer;

/**
 * @author Anton Lamtev
 */
public final class PsiUtils {
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
}
