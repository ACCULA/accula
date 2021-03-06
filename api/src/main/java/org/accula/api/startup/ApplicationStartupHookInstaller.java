package org.accula.api.startup;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.lines.LineRange;
import org.accula.api.service.CloneDetectionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Anton Lamtev
 */
@Slf4j
@Component
public final class ApplicationStartupHookInstaller extends AtomicBoolean implements ApplicationListener<ContextRefreshedEvent> {
    @Serial
    private static final long serialVersionUID = -5976516468271204267L;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (!compareAndSet(false, true)) {
            return;
        }
        final var ctx = event.getApplicationContext();
        log.info("Startup hook installation began");

        applyCodeProperties(ctx);
        fillSuffixTree(ctx);

        log.info("Startup hook installation ended");
    }

    private static void applyCodeProperties(final ApplicationContext ctx) {
        final var cache = ctx.getEnvironment().getProperty("accula.code.lines.cache", Integer.class, 0);
        System.setProperty(LineRange.Single.CACHE_SIZE_PROPERTY_KEY, cache.toString());
    }

    private static void fillSuffixTree(final ApplicationContext ctx) {
        log.info("CloneDetectionService started filling the suffix tree");
        final var cloneDetectionService = ctx.getBean(CloneDetectionService.class);
        cloneDetectionService.fillSuffixTree().block();
        log.info("CloneDetectionService filled the suffix tree successfully");
    }
}
