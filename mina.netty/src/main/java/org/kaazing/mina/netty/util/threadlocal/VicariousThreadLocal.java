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
/*
Copyright 2006 Thomas Hawtin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
/*
 According to the apache license document http://www.apache.org/licenses/LICENSE-2.0,
 we have kept the original apache licensing and only changed the package to
 org.kaazing.mina.netty.util.threadlocal. This is being tracked
 by http://jira.kaazing.wan/browse/KG-12014.
 */
package org.kaazing.mina.netty.util.threadlocal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A drop-in replacement {@code ThreadLocal} implementation that does not leak when thread-local values reference the
 * {@code ThreadLocal} object. The code is optimised to cope with frequently changing values.
 * <p>
 * In comparison to plain {@code ThreadLocal}, this implementation:
 * <ul>
 * <li>from the point of view of a single thread, each thread-local {code #get} requires access to four objects instead
 * of two
 * <li>is fractionally slower in terms of CPU cycles for {code #get}
 * <li>uses around twice the memory for each thead-local value
 * <li>uses around four times the memory for each {@code ThreadLocal}
 * <li>may release thread-local values for garbage collection more promptly
 * </ul>
 */
public class VicariousThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Maps a unique WeakReference onto each Thread.
     */
    private static final ThreadLocal<WeakReference<Thread>> weakThread = new ThreadLocal<>();

    /**
     * Returns a unique object representing the current thread. Although we use a weak-reference to the thread, we could
     * use practically anything that does not reference our class-loader.
     */
    static WeakReference<Thread> currentThreadRef() {
        WeakReference<Thread> ref = weakThread.get();
        if (ref == null) {
            ref = new WeakReference<>(Thread.currentThread());
            weakThread.set(ref);
        }
        return ref;
    }

    /**
     * Object representing an uninitialised value.
     */
    private static final Object UNINITIALISED = new Object();

    /**
     * Actual ThreadLocal implementation object.
     */
    private final ThreadLocal<WeakReference<Holder>> local = new ThreadLocal<>();

    /**
     * Maintains a strong reference to value for each thread, so long as the Thread has not been collected. Note, alive
     * Threads strongly references the WeakReference&lt;Thread> through weakThread.
     */
    private volatile Holder strongRefs;

    /**
     * Compare-and-set of {@link #strongRefs}.
     */
    private
    static final AtomicReferenceFieldUpdater<VicariousThreadLocal, Holder> strongRefsUpdater = AtomicReferenceFieldUpdater
            .newUpdater(VicariousThreadLocal.class, Holder.class, "strongRefs");

    /**
     * Queue of Holders belonging to exited threads.
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * Creates a new {@code VicariousThreadLocal}.
     */
    public static <T> VicariousThreadLocal<T> newThreadLocal() {
        return new VicariousThreadLocal<>();
    }

    /**
     * Creates a new {@code VicariousThreadLocal} with specified initial value.
     */
    public static <T> VicariousThreadLocal<T> newThreadLocal(final T initialValue) {
        return new VicariousThreadLocal<T>() {
            @Override
            public T initialValue() {
                return initialValue;
            }
        };
    }

    /**
     * Creates a new {@code VicariousThreadLocal} with initial value from {@code Callable}.
     */
    public static <T> VicariousThreadLocal<T> newThreadLocal(final java.util.concurrent.Callable<T> doCall) {
        return new VicariousThreadLocal<T>() {
            @Override
            public T initialValue() {
                try {
                    return doCall.call();
                } catch (RuntimeException exc) {
                    throw exc;
                } catch (Exception exc) {
                    throw new Error(exc);
                }
            }
        };
    }

    /**
     * Creates a new {@code VicariousThreadLocal}.
     */
    public VicariousThreadLocal() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        final Holder holder;
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            holder = ref.get();
            Object value = holder.value;
            if (value != UNINITIALISED) {
                return (T) value;
            }
        } else {
            holder = createHolder();
        }
        T value = initialValue();
        holder.value = value;
        return value;
    }

    @Override
    public void set(T value) {
        WeakReference<Holder> ref = local.get();
        final Holder holder = ref != null ? ref.get() : createHolder();
        holder.value = value;
    }

    /**
     * Creates a new holder object, and registers it appropriately. Also polls for thread-exits.
     */
    private Holder createHolder() {
        poll();
        Holder holder = new Holder(queue);
        WeakReference<Holder> ref = new WeakReference<>(holder);

        Holder old;
        do {
            old = strongRefs;
            holder.next = old;
        } while (!strongRefsUpdater.compareAndSet(this, old, holder));

        local.set(ref);
        return holder;
    }

    @Override
    public void remove() {
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            ref.get().value = UNINITIALISED;
        }
    }

    /**
     * Indicates whether thread-local has been initialised for the current thread.
     */
    public boolean isInitialized() {
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            Holder holder = ref.get();
            return holder != null && holder.value != UNINITIALISED;
        } else {
            return false;
        }
    }

    /**
     * Swaps the current threads value with the supplied value. Thread-local will be initialised if not already done so.
     */
    @SuppressWarnings("unchecked")
    public T swap(T value) {
        final Holder holder;
        final T oldValue;
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            holder = ref.get();
            Object holderValue = holder.value;
            if (holderValue != UNINITIALISED) {
                oldValue = (T) holderValue;
            } else {
                oldValue = initialValue();
            }
        } else {
            holder = createHolder();
            oldValue = initialValue();
        }
        holder.value = value;
        return oldValue;
    }

    /**
     * Executes task with thread-local set to the specified value. The state is restored however the task exits. If
     * uninitialised before running the task, it will remain so upon exiting. Setting the thread-local within the task
     * does not affect the state after exitign.
     */
    public void runWith(T value, Runnable doRun) {
        WeakReference<Holder> ref = local.get();
        Holder holder = ref != null ? ref.get() : createHolder();
        Object oldValue = holder.value;
        holder.value = value;
        try {
            doRun.run();
        } finally {
            holder.value = oldValue;
        }
    }

    /**
     * Executes task with thread-local set to the specified value. The state is restored however the task exits. If
     * uninitialised before running the task, it will remain so upon exiting. Setting the thread-local within the task
     * does not affect the state after exitign.
     * @return value returned by {@code doCall}
     */
    public <R> R callWith(T value, java.util.concurrent.Callable<R> doCall) throws Exception {
        WeakReference<Holder> ref = local.get();
        Holder holder = ref != null ? ref.get() : createHolder();
        Object oldValue = holder.value;
        holder.value = value;
        try {
            return doCall.call();
        } finally {
            holder.value = oldValue;
        }
    }

    /**
     * Check if any strong references need should be removed due to thread exit.
     */
    public void poll() {
        synchronized (queue) {
            // Remove queued references.
            // (Is this better inside or out?)
            if (queue.poll() == null) {
                // Nothing to do.
                return;
            }
            while (queue.poll() != null) {
                // Discard.
            }

            // Remove any dead holders.
            Holder first = strongRefs;
            if (first == null) {
                // Unlikely...
                return;
            }
            Holder link = first;
            Holder next = link.next;
            while (next != null) {
                if (next.get() == null) {
                    next = next.next;
                    link.next = next;
                } else {
                    link = next;
                    next = next.next;
                }
            }

            // Remove dead head, possibly.
            if (first.get() == null) {
                if (!strongRefsUpdater.weakCompareAndSet(this, first, first.next)) {
                    // Something else has come along.
                    // Just null out - next search will remove it.
                    first.value = null;
                }
            }
        }
    }

    /**
     * Holds strong reference to a thread-local value. The WeakReference is to a thread-local representing the current
     * thread.
     */
    private static class Holder extends WeakReference<Object> {
        /**
         * Construct a new holder for the current thread.
         */
        Holder(ReferenceQueue<Object> queue) {
            super(currentThreadRef(), queue);
        }

        /**
         * Next holder in chain for this thread-local.
         */
        Holder next;
        /**
         * Current thread-local value. {@link #UNINITIALISED} represents an uninitialised value.
         */
        Object value = UNINITIALISED;
    }
}
