package org.accula.api.token.kotlin;

import org.accula.api.code.FileEntity;
import org.accula.api.token.LanguageTokenProvider;
import org.accula.api.token.Token;
import org.accula.api.token.TraverseUtils;
import org.accula.api.token.kotlin.psi.KotlinPsiUtils;
import org.accula.api.token.kotlin.psi.KtPsiFactoryProvider;
import org.accula.api.util.Checks;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * @author Anton Lamtev
 */
public final class KotlinTokenProvider<Ref> implements LanguageTokenProvider<Ref> {
    @Override
    public boolean supportsFile(final FileEntity<Ref> file) {
        return file.name().endsWith(".kt");
    }

    @Override
    public Stream<List<Token<Ref>>> tokensByMethods(final FileEntity<Ref> file) {
        final var filename = Checks.notNull(file.name(), "FileEntity name");
        final var content = Checks.notNull(file.content(), "FileEntity content");
        final var psiFile = KtPsiFactoryProvider.instance().psiFactory().createFile(filename, content);

        return KotlinPsiUtils
            .methods(psiFile, file.lines()::containsAny)
            .stream()
            .map(method -> methodTokens(method, file))
            .filter(not(List::isEmpty));
    }

    private static <Ref> List<Token<Ref>> methodTokens(final KtNamedFunction method, final FileEntity<Ref> file) {
        final var body = Checks.notNull(method.getBodyExpression(), "KtNamedFunction getBodyExpression");
        final var filename = method.getContainingFile().getName();
        return TraverseUtils
            .dfs(body, TraverseUtils.stream(PsiElement::getChildren))
            .filter(KotlinPsiUtils::isValuableToken)
            .map(token -> {
                final var lineRange = KotlinPsiUtils.lineRange(token);
                if (!file.lines().containsAny(lineRange)) {
                    return null;
                }
                final var string = KotlinPsiUtils.optimizeTokenString(token.getNode().getElementType());
                return Token.of(string, filename, method.getName(), lineRange, file.ref());
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
