package org.accula.api.util;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE;
import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE;

/**
 * @author Anton Lamtev
 */
public final class ReactorSchedulers {
    private ReactorSchedulers() {
    }

    public static Scheduler boundedElastic(final Object owner) {
        return boundedElastic(owner.getClass().getSimpleName());
    }

    public static Scheduler boundedElastic(final String name) {
        return Schedulers.newBoundedElastic(
                DEFAULT_BOUNDED_ELASTIC_SIZE,
                DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                name + "-boundedElastic",
                60,
                true
        );
    }
}
