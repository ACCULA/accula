package org.accula.api.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public class Sync {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unused")
    public static <Any> Sync create(final Any any) {
        return new Sync();
    }

    public <T> Supplier<T> reading(final Supplier<T> criticalSection) {
        return () -> {
            final var readLock = lock.readLock();
            readLock.lock();
            try {
                return criticalSection.get();
            } finally {
                readLock.unlock();
            }
        };
    }

    public <T> Supplier<T> writing(final Supplier<T> criticalSection) {
        return () -> {
            final var writeLock = lock.writeLock();
            writeLock.lock();
            try {
                return criticalSection.get();
            } finally {
                writeLock.unlock();
            }
        };
    }
}
