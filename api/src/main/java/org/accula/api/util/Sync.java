package org.accula.api.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class Sync {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public <T> Supplier<T> reading(final Supplier<T> readOp) {
        return () -> {
            final var readLock = lock.readLock();
            readLock.lock();
            try {
                return readOp.get();
            } finally {
                readLock.unlock();
            }
        };
    }

    public <T> Supplier<T> writing(final Supplier<T> writeOp) {
        return () -> {
            final var writeLock = lock.writeLock();
            writeLock.lock();
            try {
                return writeOp.get();
            } finally {
                writeLock.unlock();
            }
        };
    }

    public void reading(final Runnable readOp) {
        final var readLock = lock.readLock();
        readLock.lock();
        try {
            readOp.run();
        } finally {
            readLock.unlock();
        }
    }

    public void writing(final Runnable writeOp) {
        final var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            writeOp.run();
        } finally {
            writeLock.unlock();
        }
    }
}
