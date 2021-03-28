package org.accula.api.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
public final class Sync {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public <T> Supplier<T> reading(final Supplier<T> readOp) {
        return () -> {
            readLock.lock();
            try {
                return readOp.get();
            } finally {
                readLock.unlock();
            }
        };
    }

    public <T> T read(final Supplier<T> readOp) {
        readLock.lock();
        try {
            return readOp.get();
        } finally {
            readLock.unlock();
        }
    }

    public <T> Supplier<T> writing(final Supplier<T> writeOp) {
        return () -> {
            writeLock.lock();
            try {
                return writeOp.get();
            } finally {
                writeLock.unlock();
            }
        };
    }

    public <T> T write(final Supplier<T> writeOp) {
        writeLock.lock();
        try {
            return writeOp.get();
        } finally {
            writeLock.unlock();
        }
    }
}
