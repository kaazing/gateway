/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.server.util.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * This is a fixed size array-based queue. It's a non-concurrent version of OneToOneConcurrentArrayQueue from the Nov-2013 lock
 * free training course, altered to enforce the initially specified capacity (see offer) and use long head and tail instead of
 * AtomicLong.
 */
public class FixedArrayQueue<E> implements Queue<E> {
    private final int limit;
    private final int capacity;
    private final int mask;
    private final E[] buffer;

    private long head;
    private long tail;

    @SuppressWarnings("unchecked")
    public FixedArrayQueue(final int capacity) {
        this.limit = capacity;
        this.capacity = findNextPositivePowerOfTwo(capacity);
        mask = this.capacity - 1;
        buffer = (E[]) new Object[this.capacity];
    }

    public static int findNextPositivePowerOfTwo(final int size) {
        return 1 << (32 - Integer.numberOfLeadingZeros(size - 1));
    }

    @Override
    public boolean add(final E e) {
        if (offer(e)) {
            return true;
        }

        throw new IllegalStateException("Queue is full");
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("item cannot be null");
        }

        final long currentTail = tail;
        if (currentTail - head == limit) {
            return false;
        }

        buffer[(int) currentTail & mask] = e;
        tail = currentTail + 1;

        return true;
    }

    @Override
    public E poll() {
        final long currentHead = head;
        if (tail == currentHead) {
            return null;
        }

        final int index = (int) currentHead & mask;
        final E e = buffer[index];
        buffer[index] = null;
        head = currentHead + 1;

        return e;
    }

    @Override
    public E remove() {
        final E e = poll();
        if (null == e) {
            throw new IllegalStateException("Queue is empty");
        }

        return e;
    }

    @Override
    public E element() {
        final E e = peek();
        if (null == e) {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    @Override
    public E peek() {
        return buffer[(int) head & mask];
    }

    @Override
    public int size() {
        int size;
        do {
            final long currentHead = head;
            final long currentTail = tail;
            size = (int) (currentTail - currentHead);
        }
        while (size > capacity);

        return size;
    }

    @Override
    public boolean isEmpty() {
        return tail == head;
    }

    @Override
    public boolean contains(final Object o) {
        if (null == o) {
            return false;
        }

        for (long i = head, limit = tail; i < limit; i++) {
            final E e = buffer[(int) i & mask];
            if (o.equals(e)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        for (final E o : c) {
            add(o);
        }

        return true;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}

