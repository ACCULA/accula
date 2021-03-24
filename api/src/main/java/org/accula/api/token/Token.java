package org.accula.api.token;

import com.intellij.psi.PsiElement;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.code.lines.LineRange;
import org.accula.api.token.psi.java.JavaPsiUtils;

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
    LineRange lines;

    public static <Ref> Token<Ref> of(final PsiElement token, final String filename, final String methodName, final LineRange lineRange, final Ref ref) {
        return Token.<Ref>builder()
                .ref(ref)
                .string(JavaPsiUtils.optimizeTokenString(token.getNode().getElementType()))
                .methodName(methodName)
                .filename(filename)
                .lines(lineRange)
                .build();
    }

    @Override
    public int compareTo(final Token otherToken) {
        return string.compareTo(otherToken.string);
    }
}
