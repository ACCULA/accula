package org.accula.api.token.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiMethod;
import org.accula.api.code.FileEntity;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.token.TraverseUtils;
import org.accula.api.token.psi.PsiUtils;
import org.accula.api.token.psi.java.JavaPsiUtils;
import org.accula.api.token.psi.java.PsiFileFactoryProvider;
import org.accula.api.util.Checks;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.pool.Pool;
import reactor.pool.PoolBuilder;

import java.util.List;
import java.util.Objects;

import static java.util.function.Predicate.not;

/**
 * @author Anton Lamtev
 */
public final class JavaTokenProvider<Ref> implements TokenProvider<Ref> {
    private final Pool<PsiFileFactory> fileFactoryPool = PoolBuilder
            .from(PsiFileFactoryProvider.instance().get())
            .sizeBetween(0, Runtime.getRuntime().availableProcessors())
            .buildPool();

    @Override
    public Flux<List<Token<Ref>>> tokensByMethods(final Flux<FileEntity<Ref>> files) {
        return files
                .window(Runtime.getRuntime().availableProcessors() * 4)
                .flatMap(fileFlux -> fileFactoryPool
                        .withPoolable(psiFileFactory -> fileFlux
                                .parallel(Runtime.getRuntime().availableProcessors())
                                .runOn(Schedulers.parallel())
                                .flatMap(file -> tokensFromFile(file, psiFileFactory)))
                );
    }

    private static <Ref> Flux<List<Token<Ref>>> tokensFromFile(final FileEntity<Ref> file, final PsiFileFactory psiFileFactory) {
        final var filename = Checks.notNull(file.name(), "FileEntity name");
        final var content = Checks.notNull(file.content(), "FileEntity content");
        final var methods = JavaPsiUtils
                .methods(psiFileFactory.createFileFromText(filename, JavaLanguage.INSTANCE, content), file.lines()::containsAny)
                .stream()
                .map(psiFile -> methodTokens(psiFile, file))
                .filter(not(List::isEmpty));
        return Flux.fromStream(methods);
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
                    return Token.of(token, filename, method.getName(), lineRange, file.ref());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
