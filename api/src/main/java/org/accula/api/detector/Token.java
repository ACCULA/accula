package org.accula.api.detector;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.detector.psi.PsiUtils;

import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Builder(access = PRIVATE)
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Token<Ref> implements Comparable<Token<Ref>> {
    Ref ref;
    @EqualsAndHashCode.Include
    String string;
    String methodName;
    String filename;
    int fromLine;
    int toLine;

    public static <Ref> Token<Ref> of(final PsiElement token, final Ref ref) {
        final var methodName = PsiUtils.parentsWithSelf(token)
                .filter(PsiMethod.class::isInstance)
                .findFirst()
                .map(method -> ((PsiMethod) method).getName())
                .orElseThrow();
        final var file = token.getContainingFile();
        final var document = Objects.requireNonNull(file.getViewProvider().getDocument());
        final var textRange = token.getTextRange();
        final var fromLine = document.getLineNumber(textRange.getStartOffset()) + 1;
        final var toLine = document.getLineNumber(textRange.getEndOffset()) + 1;
        return Token.<Ref>builder()
                .ref(ref)
                .string(token.getNode().getElementType().toString())
                .methodName(methodName)
                .filename(file.getName())
                .fromLine(fromLine)
                .toLine(toLine)
                .build();
    }

    @Override
    public int compareTo(final Token otherToken) {
        return string.compareTo(otherToken.string);
    }
}
