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
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel.socket.nio;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.internal.DeadLockProofWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.real_logic.agrona.concurrent.BackoffIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;

abstract class AbstractNioSelector implements NioSelector {
    protected static final Logger PERF_LOGGER = LoggerFactory.getLogger("performance.tcp");

    private static final AtomicInteger nextId = new AtomicInteger();
    protected static final long LATENCY_BEFORE_LOG_PROCESS_SELECT = MILLISECONDS.toNanos(100);
    private static final long LATENCY_BEFORE_LOG_TASK = MILLISECONDS.toNanos(100);

    private final int id = nextId.incrementAndGet();
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy(50, 50,
            NANOSECONDS.toNanos(100), MILLISECONDS.toNanos(10));

    /**
     * Internal Netty logger.
     */
    protected static final InternalLogger logger = InternalLoggerFactory
            .getInstance(AbstractNioSelector.class);

    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.

    /**
     * Executor used to execute {@link Runnable}s such as channel registration
     * task.
     */
    private final Executor executor;

    /**
     * If this worker has been started thread will be a reference to the thread
     * used when starting. i.e. the current thread when the run method is executed.
     */
    protected volatile Thread thread;

    /**
     * Count down to 0 when the I/O thread starts and {@link #thread} is set to non-null.
     */
    final CountDownLatch startupLatch = new CountDownLatch(1);

    /**
     * The NIO {@link Selector}.
     */
    protected volatile Selector selector;

    /**
     * Boolean that controls determines if a blocked Selector.select should
     * break out of its selection process. In our case we use a timeone for
     * the select method and the select method will block for that time unless
     * waken up.
     */
    protected final AtomicBoolean wakenUp = new AtomicBoolean();

    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    private volatile int cancelledKeys; // should use AtomicInteger but we just need approximation

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private volatile boolean shutdown;

    AbstractNioSelector(Executor executor) {
        this(executor, null);
    }

    AbstractNioSelector(Executor executor, ThreadNameDeterminer determiner) {
        this.executor = executor;
        openSelector(determiner);
    }

    @Override
    public void register(Channel channel, ChannelFuture future) {
        Runnable task = createRegisterTask(channel, future);
        registerTask(task);
    }

    protected final void registerTask(Runnable task) {
        taskQueue.add(task);

        Selector selector = this.selector;

        if (selector != null) {
            if (wakenUp.compareAndSet(false, true)) {
                selector.wakeup();
            }
            // log("Task added to queue");
        } else {
            if (taskQueue.remove(task)) {
                // the selector was null this means the Worker has already been shutdown.
                throw new RejectedExecutionException("Worker has already been shutdown");
            }
        }
    }

    protected final boolean isIoThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void rebuildSelector() {
        if (!isIoThread()) {
            taskQueue.add(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector();
                }
            });
            return;
        }

        final Selector oldSelector = selector;
        final Selector newSelector;

        if (oldSelector == null) {
            return;
        }

        try {
            newSelector = SelectorUtil.open();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        for (;;) {
            try {
                for (SelectionKey key: oldSelector.keys()) {
                    try {
                        if (key.channel().keyFor(newSelector) != null) {
                            continue;
                        }

                        int interestOps = key.interestOps();
                        key.cancel();
                        key.channel().register(newSelector, interestOps, key.attachment());
                        nChannels ++;
                    } catch (Exception e) {
                        logger.warn("Failed to re-register a Channel to the new Selector,", e);
                        close(key);
                    }
                }
            } catch (ConcurrentModificationException e) {
                // Probably due to concurrent modification of the key set.
                continue;
            }

            break;
        }

        selector = newSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        logger.info("Migrated " + nChannels + " channel(s) to the new Selector,");
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        startupLatch.countDown();

        int selectReturnsImmediately = 0;
        Selector selector = this.selector;

        if (selector == null) {
            return;
        }
        // use 80% of the timeout for measure
        final long minSelectTimeout = SelectorUtil.SELECT_TIMEOUT_NANOS * 80 / 100;
        boolean wakenupFromLoop = false;
        boolean quickSelect = false;
        long maximumProcessTaskQueueNanos = getMaximumProcessTaskQueueTimeNanos();
        for (;;) {
            wakenUp.set(false);

            try {
                long beforeSelect = System.nanoTime();
                int selected = select(selector, quickSelect);
                // The SelectorUtil.EPOLL_BUG_WORKAROUND condition was removed in Netty 3.10.5 and instead
                // added to the if (selectReturnsImmediately == 1024) condition later on. This seems inefficient
                // for the (common) case where the workaround is not enabled since in that case there's no point
                // in looping through the selector keys, so we are keeping the condition here. There's no risk
                // of a busy loop (https://github.com/netty/netty/issues/2426) when the workaround is not enabled.
                if (SelectorUtil.EPOLL_BUG_WORKAROUND && selected == 0 && !wakenupFromLoop && !wakenUp.get()) {
                    long timeBlocked = System.nanoTime() - beforeSelect;

                    if (timeBlocked < minSelectTimeout) {
                        boolean notConnected = false;
                        // loop over all keys as the selector may was unblocked because of a closed channel
                        for (SelectionKey key: selector.keys()) {
                            SelectableChannel ch = key.channel();
                            try {
                                if (ch instanceof DatagramChannel && !ch.isOpen() ||
                                        ch instanceof SocketChannel && !((SocketChannel) ch).isConnected() &&
                                                // Only cancel if the connection is not pending
                                                // See https://github.com/netty/netty/issues/2931
                                                !((SocketChannel) ch).isConnectionPending()) {
                                    notConnected = true;
                                    // cancel the key just to be on the safe side
                                    key.cancel();
                                }
                            } catch (CancelledKeyException e) {
                                // ignore
                            }
                        }
                        if (notConnected) {
                            selectReturnsImmediately = 0;
                        } else {
                            if (Thread.interrupted() && !shutdown) {
                                // Thread was interrupted but NioSelector was not shutdown.
                                // As this is most likely a bug in the handler of the user or it's client
                                // library we will log it.
                                //
                                // See https://github.com/netty/netty/issues/2426
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Selector.select() returned prematurely because the I/O thread " +
                                            "has been interrupted. Use shutdown() to shut the NioSelector down.");
                                }
                                selectReturnsImmediately = 0;
                            } else {
                                // Returned before the minSelectTimeout elapsed with nothing selected.
                                // This may be because of a bug in JDK NIO Selector provider, so increment the counter
                                // which we will use later to see if it's really the bug in JDK.
                                selectReturnsImmediately ++;
                            }
                        }
                    } else {
                        selectReturnsImmediately = 0;
                    }

                    if (selectReturnsImmediately == 1024) {
                        // The selector returned immediately for 10 times in a row,
                        // so recreate one selector as it seems like we hit the
                        // famous epoll(..) jdk bug.
                        rebuildSelector();
                        selector = this.selector;
                        selectReturnsImmediately = 0;
                        wakenupFromLoop = false;
                        // try to select again
                        continue;
                    }
                } else {
                    // reset counter
                    selectReturnsImmediately = 0;
                }

                // 'wakenUp.compareAndSet(false, true)' is always evaluated
                // before calling 'selector.wakeup()' to reduce the wake-up
                // overhead. (Selector.wakeup() is an expensive operation.)
                //
                // However, there is a race condition in this approach.
                // The race condition is triggered when 'wakenUp' is set to
                // true too early.
                //
                // 'wakenUp' is set to true too early if:
                // 1) Selector is waken up between 'wakenUp.set(false)' and
                //    'selector.select(...)'. (BAD)
                // 2) Selector is waken up between 'selector.select(...)' and
                //    'if (wakenUp.get()) { ... }'. (OK)
                //
                // In the first case, 'wakenUp' is set to true and the
                // following 'selector.select(...)' will wake up immediately.
                // Until 'wakenUp' is set to false again in the next round,
                // 'wakenUp.compareAndSet(false, true)' will fail, and therefore
                // any attempt to wake up the Selector will fail, too, causing
                // the following 'selector.select(...)' call to block
                // unnecessarily.
                //
                // To fix this problem, we wake up the selector again if wakenUp
                // is true immediately after selector.select(...).
                // It is inefficient in that it wakes up the selector for both
                // the first case (BAD - wake-up required) and the second case
                // (OK - no wake-up required).

                if (wakenUp.get()) {
                    wakenupFromLoop = true;
                    selector.wakeup();
                } else {
                    wakenupFromLoop = false;
                }

                cancelledKeys = 0;
                if (maximumProcessTaskQueueNanos > 0) {
                    long deadlineNanos = System.nanoTime() + maximumProcessTaskQueueNanos;
                    quickSelect = processTaskQueue(deadlineNanos);
                }
                else {
                    processTaskQueue();
                }
                selector = this.selector; // processTaskQueue() can call rebuildSelector()

                if (shutdown) {
                    this.selector = null;

                    // process one time again
                    processTaskQueue();

                    for (SelectionKey k: selector.keys()) {
                        try {
                            close(k);
                        } catch (Throwable e) {
                            logger.warn("Failed to close a selection key.", e);
                        }
                    }

                    try {
                        selector.close();
                    } catch (Throwable e) {
                        logger.warn(
                                "Failed to close a selector.", e);
                    }
                    shutdownLatch.countDown();
                    break;
                } else {
                    process(selector);
                    processRead();
                }
            } catch (Throwable t) {
                logger.warn(
                        "Unexpected exception in the selector loop.", t);
                PERF_LOGGER.warn(format("Unexpected exception %s in selector loop", t));
                t.printStackTrace();
                // Prevent possible consecutive immediate failures that lead to
                // excessive CPU consumption.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
        }
    }

    /**
     * Start the {@link AbstractNioWorker} and return the {@link Selector} that will be used for
     * the {@link AbstractNioChannel}'s when they get registered
     */
    private void openSelector(ThreadNameDeterminer determiner) {
        try {
            selector = SelectorUtil.open();
        } catch (Throwable t) {
            throw new ChannelException("Failed to create a selector.", t);
        }

        // Start the worker thread with the new Selector.
        boolean success = false;
        try {
            DeadLockProofWorker.start(executor, newThreadRenamingRunnable(id, determiner));
            success = true;
        } finally {
            if (!success) {
                // Release the Selector if the execution fails.
                try {
                    selector.close();
                } catch (Throwable t) {
                    logger.warn("Failed to close a selector.", t);
                }
                selector = null;
                // The method will return to the caller at this point.
            }
        }
        assert selector != null && selector.isOpen();
    }

    protected void processTaskQueue() {
        for (;;) {
            final Runnable task = taskQueue.poll();
            if (task == null) {
                break;
            }
            task.run();;

            try {
                cleanUpCancelledKeys();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private boolean processTaskQueue(long deadLineNanos) {
        int numTasks = 0;
        boolean perfLogEnabled = PERF_LOGGER.isInfoEnabled();
        long startTime = perfLogEnabled ? System.nanoTime() : 0;
        boolean quickSelect;
        for (;;) {
            final Runnable task = taskQueue.poll();
            if (task == null) {
                quickSelect = false;
                break;
            }
            numTasks++;
            task.run();

            try {
                cleanUpCancelledKeys();
            } catch (IOException e) {
                // Ignore
            }
            long now = System.nanoTime();
            if (now > deadLineNanos) {
                if (perfLogEnabled) {
                    long timeSoFar = now - startTime;
                    if (PERF_LOGGER.isDebugEnabled() || timeSoFar > LATENCY_BEFORE_LOG_TASK) {
                       PERF_LOGGER.info(format(
                               "AbstractyNioSelector.processTaskQueue: exiting after processing %d tasks in %d ms",
                               numTasks, TimeUnit.NANOSECONDS.toMillis(timeSoFar)));
                    }
                }
                // Make sure select in run() loop is no wait or short since we still have tasks to do
                quickSelect = true;
                break;
            }
        }
        return quickSelect;
    }

    protected final void increaseCancelledKeys() {
        cancelledKeys ++;
    }

    protected final boolean cleanUpCancelledKeys() throws IOException {
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            selector.selectNow();
            return true;
        }
        return false;
    }

    @Override
    public void shutdown() {
        if (isIoThread()) {
            throw new IllegalStateException("Must not be called from a I/O-Thread to prevent deadlocks!");
        }

        Selector selector = this.selector;
        shutdown = true;
        if (selector != null) {
            selector.wakeup();
        }
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Interrupted while wait for resources to be released #" + id);
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void process(Selector selector) throws IOException;

    protected void processRead() throws IOException {
    }

    protected int select(Selector selector, boolean quickSelect) throws IOException {
        return select(selector);
    }

    protected int select(Selector selector) throws IOException {
        return SelectorUtil.select(selector);
    }

    protected abstract void close(SelectionKey k);

    protected abstract ThreadRenamingRunnable newThreadRenamingRunnable(int id, ThreadNameDeterminer determiner);

    protected abstract Runnable createRegisterTask(Channel channel, ChannelFuture future);

    protected long getMaximumProcessTaskQueueTimeNanos() {
        return 0; // no limit (process all tasks)
    }
}
