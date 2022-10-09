package org.accula.api.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Duration;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
public final class MemoryUsageLogger {
    @PostConstruct
    void init() {
        Flux.interval(Duration.ofMinutes(1L), Schedulers.single())
            .doOnNext(__ -> {
                final var memory = ManagementFactory.getMemoryMXBean();
                record MemUsage(MemoryUsage mem) {
                    @Override
                    public String toString() {
                        return """
                            <
                            init=%s
                            used=%s
                            commited=%s
                            max=%s
                            >
                            """.formatted(gbs(mem.getInit()), gbs(mem.getUsed()), gbs(mem.getCommitted()), gbs(mem.getMax()));
                    }

                    static String gbs(final long bytes) {
                        return "%.2fGB".formatted((double) bytes / (1024 * 1024 * 1024));
                    }
                }
                log.info("Heap usage = {}", new MemUsage(memory.getHeapMemoryUsage()));
                log.info("Non heap usage = {}", new MemUsage(memory.getNonHeapMemoryUsage()));
            })
            .subscribe();
    }
}
