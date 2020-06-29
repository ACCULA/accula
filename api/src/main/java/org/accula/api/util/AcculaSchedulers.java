package org.accula.api.util;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE;
import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE;

public final class AcculaSchedulers {
    private AcculaSchedulers() {
    }

    public static Scheduler newBoundedElastic(final String name) {
        return Schedulers.newBoundedElastic(
                DEFAULT_BOUNDED_ELASTIC_SIZE,
                DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                String.format("%s-boundedElastic", name),
                60,
                true
        );
    }
}
