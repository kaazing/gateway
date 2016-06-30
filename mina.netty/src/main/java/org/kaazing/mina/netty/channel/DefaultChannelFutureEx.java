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
package org.kaazing.mina.netty.channel;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.netty.channel.DefaultChannelFuture.isUseDeadLockChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.internal.DeadLockProofWorker;

/**
 * The default {@link ChannelFuture} implementation.  It is recommended to
 * use {@link Channels#future(Channel)} and {@link Channels#future(Channel, boolean)}
 * to create a new {@link ChannelFuture} rather than calling the constructor
 * explicitly.
 */
public class DefaultChannelFutureEx implements ChannelFuture {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(DefaultChannelFuture.class);

    private static final Throwable CANCELLED = new Throwable();

    private Channel channel;
    private boolean cancellable;

    private ChannelFutureListener firstListener;
    private List<ChannelFutureListener> otherListeners;
    private List<ChannelFutureProgressListener> progressListeners;
    private boolean done;
    private Throwable cause;
    private int waiters;

    public synchronized boolean isResetable() {
        return (done && waiters == 0) || channel == null;
    }

    /**
     * Creates a new instance.
     *
     * @param channel
     *        the {@link Channel} associated with this future
     * @param cancellable
     *        {@code true} if and only if this future can be canceled
     */
    public synchronized void reset(Channel channel, boolean cancellable) {
        this.channel = channel;
        this.cancellable = cancellable;

        assert this.firstListener == null;
        assert this.otherListeners == null || this.otherListeners.isEmpty();
        assert this.progressListeners == null || this.progressListeners.isEmpty();
        this.done = false;
        this.cause = null;
        assert this.waiters == 0;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public synchronized boolean isSuccess() {
        return done && cause == null;
    }

    @Override
    public synchronized Throwable getCause() {
        if (cause != CANCELLED) {
            return cause;
        } else {
            return null;
        }
    }

    @Override
    public synchronized boolean isCancelled() {
        return cause == CANCELLED;
    }

    @Override
    public void addListener(ChannelFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (this) {
            if (done) {
                notifyNow = true;
            } else {
                if (firstListener == null) {
                    firstListener = listener;
                } else {
                    if (otherListeners == null) {
                        otherListeners = new ArrayList<>(1);
                    }
                    otherListeners.add(listener);
                }

                if (listener instanceof ChannelFutureProgressListener) {
                    if (progressListeners == null) {
                        progressListeners = new ArrayList<>(1);
                    }
                    progressListeners.add((ChannelFutureProgressListener) listener);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    @Override
    public void removeListener(ChannelFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (this) {
            if (!done) {
                if (listener == firstListener) {
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }

                if (listener instanceof ChannelFutureProgressListener) {
                    progressListeners.remove(listener);
                }
            }
        }
    }

    @Override
    public ChannelFuture sync() throws InterruptedException {
        await();
        rethrowIfFailed0();
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        awaitUninterruptibly();
        rethrowIfFailed0();
        return this;
    }

    private void rethrowIfFailed0() {
        Throwable cause = getCause();
        if (cause == null) {
            return;
        }

        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }

        if (cause instanceof Error) {
            throw (Error) cause;
        }

        throw new ChannelException(cause);
    }

    @Override
    public ChannelFuture await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        synchronized (this) {
            while (!done) {
                checkDeadLock();
                waiters++;
                try {
                    wait();
                } finally {
                    waiters--;
                }
            }
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(MILLISECONDS.toNanos(timeoutMillis), true);
    }

    @Override
    public ChannelFuture awaitUninterruptibly() {
        boolean interrupted = false;
        synchronized (this) {
            while (!done) {
                checkDeadLock();
                waiters++;
                try {
                    wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                } finally {
                    waiters--;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toNanos(timeout), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(MILLISECONDS.toNanos(timeoutMillis), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException();
        }

        long startTime = timeoutNanos <= 0 ? 0 : System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;

        try {
            synchronized (this) {
                if (done || waitTime <= 0) {
                    return done;
                }

                checkDeadLock();
                waiters++;
                try {
                    for (;;) {
                        try {
                            wait(waitTime / 1000000, (int) (waitTime % 1000000));
                        } catch (InterruptedException e) {
                            if (interruptable) {
                                throw e;
                            } else {
                                interrupted = true;
                            }
                        }

                        if (done) {
                            return true;
                        } else {
                            waitTime = timeoutNanos - (System.nanoTime() - startTime);
                            if (waitTime <= 0) {
                                return done;
                            }
                        }
                    }
                } finally {
                    waiters--;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void checkDeadLock() {
        if (isUseDeadLockChecker() && DeadLockProofWorker.PARENT.get() != null) {
            throw new IllegalStateException(
                    "await*() in I/O thread causes a dead lock or " +
                    "sudden performance drop. Use addListener() instead or " +
                    "call await*() from a different thread.");
        }
    }

    @Override
    public boolean setSuccess() {
        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }

        notifyListeners();
        return true;
    }

    @Override
    public boolean setFailure(Throwable cause) {
        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            this.cause = cause;
            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }

        notifyListeners();
        return true;
    }

    @Override
    public boolean cancel() {
        if (!cancellable) {
            return false;
        }

        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            cause = CANCELLED;
            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }

        notifyListeners();
        return true;
    }

    private void notifyListeners() {
        // This method doesn't need synchronization because:
        // 1) This method is always called after synchronized (this) block.
        //    Hence any listener list modification happens-before this method.
        // 2) This method is called only when 'done' is true.  Once 'done'
        //    becomes true, the listener list is never modified - see add/removeListener()
        ChannelFutureListener firstListener = this.firstListener;
        if (firstListener != null) {
            // clear internal state first in case listener causes future to be reset
            List<ChannelFutureListener> otherListeners = this.otherListeners;
            List<ChannelFutureProgressListener> progressListeners = this.progressListeners;
            this.firstListener = null;
            this.otherListeners = null;
            if (progressListeners != null) {
                 progressListeners.clear();
            }

            notifyListener(firstListener);

            if (otherListeners != null) {
                for (ChannelFutureListener l: otherListeners) {
                    notifyListener(l);
                }
            }
        }
    }

    private void notifyListener(ChannelFutureListener l) {
        try {
            l.operationComplete(this);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        "An exception was thrown by " +
                        ChannelFutureListener.class.getSimpleName() + '.', t);
            }
        }
    }

    @Override
    public boolean setProgress(long amount, long current, long total) {
        ChannelFutureProgressListener[] plisteners;
        synchronized (this) {
            // Do not generate progress event after completion.
            if (done) {
                return false;
            }

            Collection<ChannelFutureProgressListener> progressListeners =
                this.progressListeners;
            if (progressListeners == null || progressListeners.isEmpty()) {
                // Nothing to notify - no need to create an empty array.
                return true;
            }

            plisteners = progressListeners.toArray(
                    new ChannelFutureProgressListener[progressListeners.size()]);
        }

        for (ChannelFutureProgressListener pl: plisteners) {
            notifyProgressListener(pl, amount, current, total);
        }

        return true;
    }

    private void notifyProgressListener(
            ChannelFutureProgressListener l,
            long amount, long current, long total) {

        try {
            l.operationProgressed(this, amount, current, total);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        "An exception was thrown by " +
                        ChannelFutureProgressListener.class.getSimpleName() + '.', t);
            }
        }
    }
}
