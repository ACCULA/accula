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
                            init=%.2fGB
                            max=%.2fGB
                            commited=%.2fGB
                            used=%.2fGB
                            >
                            """.formatted(gbs(mem.getInit()), gbs(mem.getMax()), gbs(mem.getCommitted()), gbs(mem.getUsed()));
                    }

                    static float gbs(final long bytes) {
                        return (float) bytes / (1024 * 1024 * 1024);
                    }
                }
                log.info("Heap usage = {}", new MemUsage(memory.getHeapMemoryUsage()));
                log.info("Non heap usage = {}", new MemUsage(memory.getNonHeapMemoryUsage()));
            })
            .subscribe();
    }
}
