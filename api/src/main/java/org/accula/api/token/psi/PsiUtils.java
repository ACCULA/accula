package org.accula.api.token.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.accula.api.code.lines.LineRange;
import org.accula.api.util.Checking;

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

    public static LineRange lineRange(final PsiElement element) {
        final var file = element.getContainingFile();
        final var doc = Checking.notNull(file.getViewProvider().getDocument(), "Document");
        final var relativeRange = element.getTextRange();
        return LineRange.of(
                doc.getLineNumber(relativeRange.getStartOffset()) + 1,
                doc.getLineNumber(relativeRange.getEndOffset()) + 1
        );
    }
}
