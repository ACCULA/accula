package org.accula.api.token.psi.java;

import com.intellij.core.CoreProjectEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFileFactory;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class PsiFileFactoryProvider implements Supplier<Mono<PsiFileFactory>> {
    private final CoreProjectEnvironment projectEnvironment;

    private PsiFileFactoryProvider() {
        final var appEnvDisposable = Disposer.newDisposable("appEnvDisposable");
        final var appEnv = JavaApplicationEnvironment.of(appEnvDisposable);
        final var projectEnvDisposable = Disposer.newDisposable(appEnvDisposable, "projectEnvDisposable");
        this.projectEnvironment = new JavaCoreProjectEnvironment(projectEnvDisposable, appEnv);
    }

    public static PsiFileFactoryProvider instance() {
        return Holder.INSTANCE;
    }

    @Override
    public Mono<PsiFileFactory> get() {
        return Mono.just(PsiFileFactory.getInstance(projectEnvironment.getProject()));
    }

    private static final class Holder {
        private static final PsiFileFactoryProvider INSTANCE = new PsiFileFactoryProvider();

        private Holder() {
        }
    }
}
