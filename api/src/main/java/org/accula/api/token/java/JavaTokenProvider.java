package org.accula.api.token.java;

import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiMethod;
import org.accula.api.code.FileEntity;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.token.TraverseUtils;
import org.accula.api.token.psi.java.JavaApplicationEnvironment;
import org.accula.api.token.psi.java.JavaPsiUtils;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
public final class JavaTokenProvider<Ref> implements TokenProvider<Ref> {
    @Override
    public Flux<List<Token<Ref>>> tokensByMethods(final Flux<FileEntity<Ref>> files) {
        return javaPsiFileFactory().flatMapMany(psiFileFactory ->
                files.flatMapIterable(file ->
                        JavaPsiUtils
                                .methods(psiFileFactory.createFileFromText(file.getName(), JavaLanguage.INSTANCE, file.getContent()))
                                .stream()
                                .map(Lambda.passingTailArg(JavaTokenProvider::methodTokens, file))
                                .collect(toList())));
    }

    private static <Ref> List<Token<Ref>> methodTokens(final PsiMethod method, final FileEntity<Ref> file) {
        return TraverseUtils
                .dfs(method.getBody(), TraverseUtils.stream(PsiElement::getChildren))
                .filter(JavaPsiUtils::isValuableToken)
                .map(Lambda.passingTailArgs(Token::of, method.getName(), file.getRef()))
                .collect(toList());
    }

    private static Mono<PsiFileFactory> javaPsiFileFactory() {
        final var disposable = Disposer.newDisposable();
        return Mono
                .fromSupplier(() -> {
                    final var appEnv = JavaApplicationEnvironment.of(disposable, true);
                    final var projectEnv = new JavaCoreProjectEnvironment(disposable, appEnv);
                    @SuppressWarnings("deprecation")//
                    final var psiElfEpName = PsiElementFinder.EP_NAME;
                    projectEnv.registerProjectExtensionPoint(psiElfEpName, PsiElementFinder.class);
                    return PsiFileFactory.getInstance(projectEnv.getProject());
                })
                .doFinally(__ -> disposable.dispose());
    }
}
