package org.accula.api.token.kotlin.psi;

import lombok.Getter;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import static org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.GRADLE_STYLE;
import static org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES;

/**
 * @author Anton Lamtev
 */
public final class KtPsiFactoryProvider {
    private static final KtPsiFactoryProvider INSTANCE = new KtPsiFactoryProvider();
    @Getter(lazy = true)
    private final KtPsiFactory psiFactory = createPsiFactory();

    private KtPsiFactoryProvider() {
    }

    public static KtPsiFactoryProvider instance() {
        return INSTANCE;
    }

    private static KtPsiFactory createPsiFactory() {
        final var disposable = Disposer.newDisposable("KotlinCoreEnvironment disposable");
        final var compilerConf = new CompilerConfiguration();
        compilerConf.putIfNotNull(MESSAGE_COLLECTOR_KEY, new PrintingMessageCollector(System.err, GRADLE_STYLE, false));
        final var env = KotlinCoreEnvironment.createForProduction(disposable, compilerConf, JVM_CONFIG_FILES);
        return new KtPsiFactory(env.getProject(), true);
    }
}
