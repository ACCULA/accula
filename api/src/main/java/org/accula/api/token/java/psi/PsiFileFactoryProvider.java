package org.accula.api.token.java.psi;

import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFileFactory;
import lombok.Getter;

/**
 * @author Anton Lamtev
 */
public final class PsiFileFactoryProvider {
    private static final PsiFileFactoryProvider INSTANCE = new PsiFileFactoryProvider();
    @Getter(lazy = true)
    private final PsiFileFactory fileFactory = createFileFactory();

    private PsiFileFactoryProvider() {
    }

    public static PsiFileFactoryProvider instance() {
        return INSTANCE;
    }

    private static PsiFileFactory createFileFactory() {
        final var appEnvDisposable = Disposer.newDisposable("appEnvDisposable");
        final var appEnv = JavaApplicationEnvironment.of(appEnvDisposable);
        final var projectEnvDisposable = Disposer.newDisposable(appEnvDisposable, "projectEnvDisposable");
        final var projectEnvironment = new JavaCoreProjectEnvironment(projectEnvDisposable, appEnv);
        return PsiFileFactory.getInstance(projectEnvironment.getProject());
    }
}
