/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.ExceptionMonitor;

public class ChannelWriteFuture implements WriteFuture {

    private final IoSession session;
    private final ChannelFuture future;
    
    private final Object lock;
    private IoFutureListener<?> firstListener;
    private List<IoFutureListener<?>> otherListeners;
    private ChannelFutureListener notifier;
    
    public ChannelWriteFuture(IoSession session, ChannelFuture future) {
        this.session = session;
        this.future = future;
        this.lock = new Object();
    }
    
    @Override
    public IoSession getSession() {
        return session;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return future.await(timeout, unit);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return future.await(timeoutMillis);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return future.awaitUninterruptibly(timeout, unit);
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return future.awaitUninterruptibly(timeoutMillis);
    }

    /**
     * @deprecated Replaced with {@link #awaitUninterruptibly()}.
     */
    @Deprecated
    public void join() {
        awaitUninterruptibly();
    }

    /**
     * @deprecated Replaced with {@link #awaitUninterruptibly(long)}.
     */
    @Deprecated
    public boolean join(long timeoutMillis) {
        return awaitUninterruptibly(timeoutMillis);
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public boolean isWritten() {
        return future.isSuccess();
    }

    @Override
    public Throwable getException() {
        return future.cause();
    }

    @Override
    public void setWritten() {
        future.setSuccess();
    }

    @Override
    public void setException(Throwable cause) {
        future.setFailure(cause);
    }

    @Override
    public WriteFuture await() throws InterruptedException {
        future.await();
        return this;
    }

    @Override
    public WriteFuture awaitUninterruptibly() {
        future.awaitUninterruptibly();
        return this;
    }

    @Override
    public WriteFuture addListener(IoFutureListener<?> listener) {
        
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (future.isDone()) {
                notifyNow = true;
            }
            else {
                // add listeners locally first
                if (firstListener == null) {
                    firstListener = listener;
                }
                else {
                    if (otherListeners == null) {
                        if (otherListeners == null) {
                            otherListeners = new ArrayList<IoFutureListener<?>>(1);
                        }
                        otherListeners.add(listener);
                    }
                }

                // then ensure notifier is attached to inner future
                if (notifier == null) {
                    notifier = new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            notifyListeners();
                        }
                    };
                    future.addListener(notifier);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
        
        return this;
    }

    @Override
    public WriteFuture removeListener(IoFutureListener<?> listener) {

        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (lock) {
            if (!future.isDone()) {
                if (listener == firstListener) {
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }
            }
        }

        return this;
    }

    private void notifyListeners() {
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;

            if (otherListeners != null) {
                for (IoFutureListener<?> l : otherListeners) {
                    notifyListener(l);
                }
                otherListeners = null;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void notifyListener(IoFutureListener l) {
        try {
            l.operationComplete(this);
        }
        catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }
    }
}
