package org.accula.api.token.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.accula.api.code.FileEntity;
import org.accula.api.token.LanguageTokenProvider;
import org.accula.api.token.Token;
import org.accula.api.token.TraverseUtils;
import org.accula.api.token.java.psi.JavaPsiUtils;
import org.accula.api.token.java.psi.PsiFileFactoryProvider;
import org.accula.api.token.psi.PsiUtils;
import org.accula.api.util.Checks;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * @author Anton Lamtev
 */
public final class JavaTokenProvider<Ref> implements LanguageTokenProvider<Ref> {
    @Override
    public boolean supportsFile(final FileEntity<Ref> file) {
        return file.name().endsWith(".java");
    }

    @Override
    public Stream<List<Token<Ref>>> tokensByMethods(final FileEntity<Ref> file) {
        final var filename = Checks.notNull(file.name(), "FileEntity name");
        final var content = Checks.notNull(file.content(), "FileEntity content");
        final var psiFile = PsiFileFactoryProvider.instance().fileFactory().createFileFromText(filename, JavaLanguage.INSTANCE, content);

        return JavaPsiUtils
            .methods(psiFile, file.lines()::containsAny)
            .stream()
            .map(method -> methodTokens(method, file))
            .filter(not(List::isEmpty));
    }

    private static <Ref> List<Token<Ref>> methodTokens(final PsiMethod method, final FileEntity<Ref> file) {
        final var body = Checks.notNull(method.getBody(), "PsiMethod body");
        final var filename = method.getContainingFile().getName();
        return TraverseUtils
                .dfs(body, TraverseUtils.stream(PsiElement::getChildren))
                .filter(JavaPsiUtils::isValuableToken)
                .map(token -> {
                    final var lineRange = PsiUtils.lineRange(token);
                    if (!file.lines().containsAny(lineRange)) {
                        return null;
                    }
                    final var string = JavaPsiUtils.optimizeTokenString(token.getNode().getElementType());
                    return Token.of(string, filename, method.getName(), lineRange, file.ref());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
