package org.accula.api.startup;

import org.accula.api.service.CloneDetectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Anton Lamtev
 */
public final class ApplicationStartupHookInstaller {
    private static final AtomicBoolean contextRefreshed = new AtomicBoolean(false);

    private ApplicationStartupHookInstaller() {
    }

    public static void installInto(final SpringApplication app) {
        app.addListeners((final ContextRefreshedEvent event) -> {
            if (!contextRefreshed.compareAndSet(false, true)) {
                return;
            }
            fillSuffixTree(event.getApplicationContext());
        });
    }

    private static void fillSuffixTree(final ApplicationContext ctx) {
        final var cloneDetectionService = ctx.getBean(CloneDetectionService.class);
        cloneDetectionService.fillSuffixTree().block();
    }
}
