package org.accula.api.util;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Anton Lamtev
 */
public final class Iterators {
    private Iterators() {
    }

    public static <T> NextResettableIterator<T> nextResettable(final Iterator<T> iterator) {
        return new NextResettableIteratorImpl<>(iterator);
    }

    public static <T> Iterator<T> withHead(final Iterator<T> iterator, final T head) {
        return new HeadIterator<>(iterator, head);
    }

    public interface NextResettableIterator<T> extends Iterator<T> {
        void resetNext(T next);
    }

    @RequiredArgsConstructor
    private static final class NextResettableIteratorImpl<T> implements NextResettableIterator<T> {
        private final Iterator<T> iterator;
        @Nullable
        private T next;

        @Override
        public boolean hasNext() {
            return next != null || iterator.hasNext();
        }

        @Override
        public T next() {
            if (next != null) {
                try {
                    return next;
                } finally {
                    next = null;
                }
            }
            return iterator.next();
        }

        @Override
        public void resetNext(final T next) {
            this.next = next;
        }
    }

    @AllArgsConstructor
    private static final class HeadIterator<T> implements Iterator<T> {
        private final Iterator<T> iterator;
        @Nullable
        private T head;

        @Override
        public boolean hasNext() {
            return head != null || iterator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (head != null) {
                try {
                    return head;
                } finally {
                    head = null;
                }
            }
            return iterator.next();
        }
    }
}
