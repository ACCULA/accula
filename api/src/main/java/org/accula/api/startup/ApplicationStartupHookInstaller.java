package org.accula.api.startup;

import org.accula.api.service.CloneDetectionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Anton Lamtev
 */
@Component
public final class ApplicationStartupHookInstaller implements ApplicationListener<ContextRefreshedEvent> {
    private static final AtomicBoolean contextRefreshed = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (!contextRefreshed.compareAndSet(false, true)) {
            return;
        }
        fillSuffixTree(event.getApplicationContext());
    }

    private static void fillSuffixTree(final ApplicationContext ctx) {
        final var cloneDetectionService = ctx.getBean(CloneDetectionService.class);
        cloneDetectionService.fillSuffixTree().block();
    }
}
