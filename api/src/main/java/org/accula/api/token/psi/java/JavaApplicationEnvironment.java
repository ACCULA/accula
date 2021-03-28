package org.accula.api.token.psi.java;

import com.intellij.DynamicBundle;
import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.lang.MetaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.FileContextProvider;
import com.intellij.psi.JavaModuleSystem;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.meta.MetaDataContributor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment}-like
 * core application environment that registers all extensions required
 * to enable access to useful {@link com.intellij.psi.impl.source.PsiClassImpl} methods
 * like {@link PsiClassImpl#getAllMethods()}, {@link PsiClassImpl#getAllInnerClasses()}, etc.
 *
 * @author Anton Lamtev
 */
final class JavaApplicationEnvironment extends JavaCoreApplicationEnvironment {
    private static final AtomicBoolean areExtensionsRegistered = new AtomicBoolean(false);

    private JavaApplicationEnvironment(final Disposable disposable, final boolean unitTestMode) {
        super(disposable, unitTestMode);
    }

    static JavaApplicationEnvironment of(final Disposable disposable) {
        final var env = new JavaApplicationEnvironment(disposable, true);
        registerExtensionPoints();
        return env;
    }

    @SuppressWarnings({"Deprecated", "UnstableApiUsage"})
    private static void registerExtensionPoints() {
        if (areExtensionsRegistered.get()) {
            return;
        }
        synchronized (areExtensionsRegistered) {
            if (areExtensionsRegistered.get()) {
                return;
            }
            registerApplicationExtensionPoint(DynamicBundle.LanguageBundleEP.EP_NAME, DynamicBundle.LanguageBundleEP.class);
            registerApplicationExtensionPoint(FileContextProvider.EP_NAME, FileContextProvider.class);
            registerApplicationExtensionPoint(PsiElementFinder.EP_NAME, PsiElementFinder.class);
            registerApplicationExtensionPoint(MetaDataContributor.EP_NAME, MetaDataContributor.class);
            registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
            registerApplicationExtensionPoint(JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);
            registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider.class);
            registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage.class);
            registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy.class);
            registerExtensionPoint(Extensions.getRootArea(), JavaModuleSystem.EP_NAME, JavaModuleSystem.class);
            areExtensionsRegistered.compareAndSet(false, true);
        }
    }
}
