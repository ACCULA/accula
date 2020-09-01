package org.accula.api.detector.psi;

import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFileFactory;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public final class PsiFileFactoryProvider {
    private PsiFileFactoryProvider() {
    }

    public static <R> Mono<R> using(final Function<PsiFileFactory, Mono<R>> fileFactoryUse) {
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
                .flatMap(fileFactoryUse)
                .doFinally(__ -> disposable.dispose());
    }
}
