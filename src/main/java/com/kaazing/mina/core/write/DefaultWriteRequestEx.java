/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.write;

import static java.util.Collections.unmodifiableList;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.future.DefaultWriteFutureEx;
import com.kaazing.mina.core.future.WriteFutureEx;

/**
 * Extended version of WriteRequest to add support for mutating the
 * message during encoding to avoid undesirable allocation.
 */
public class DefaultWriteRequestEx implements WriteRequestEx {

    private static final WriteFutureEx UNUSED_FUTURE = new WriteFutureEx() {
        public boolean isWritten() {
            return false;
        }

        public void setWritten() {
            // Do nothing
        }

        public IoSession getSession() {
            return null;
        }

        public void join() {
            // Do nothing
        }

        public boolean join(long timeoutInMillis) {
            return true;
        }

        public boolean isDone() {
            return true;
        }

        @Override
        public boolean isResetable() {
            return false;
        }

        @Override
        public void reset(IoSession session) {
            throw new IllegalStateException(
                    "You can't reset a dummy future.");
        }

        public WriteFuture addListener(IoFutureListener<?> listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        public WriteFuture removeListener(IoFutureListener<?> listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        public WriteFuture await() throws InterruptedException {
            return this;
        }

        public boolean await(long timeout, TimeUnit unit)
                throws InterruptedException {
            return true;
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }

        public Throwable getException() {
            return null;
        }

        public void setException(Throwable cause) {
            // Do nothing
        }
    };

    private Object message;
    private SocketAddress destination;
    private final WriteFutureEx future;

    /**
     * Creates a new instance without {@link WriteFuture}.  You'll get
     * an instance of {@link WriteFuture} even if you called this constructor
     * because {@link #getFuture()} will return a bogus future.
     */
    public DefaultWriteRequestEx(Object message) {
        this(message, null, null);
    }

    /**
     * Creates a new instance with {@link WriteFuture}.
     */
    public DefaultWriteRequestEx(Object message, WriteFutureEx future) {
        this(message, future, null);
    }

    /**
     * Creates a new instance.
     *
     * @param message a message to write
     * @param future a future that needs to be notified when an operation is finished
     * @param destination the destination of the message.  This property will be
     *                    ignored unless the transport supports it.
     */
    public DefaultWriteRequestEx(Object message, WriteFutureEx future,
            SocketAddress destination) {
        if (message == null) {
            throw new NullPointerException("message");
        }

        if (future == null) {
            future = UNUSED_FUTURE;
        }

        this.message = message;
        this.future = future;
        this.destination = destination;
    }

    private DefaultWriteRequestEx(WriteFutureEx future) {
        this.future = future;
    }

    @Override
    public boolean isResetable() {
        return future.isResetable();
    }

    @Override
    public void reset(IoSession session, Object message) {
        reset(session, message, null);
    }


    @Override
    public void reset(IoSession session, Object message, SocketAddress destination) {
        if (message == null) {
            throw new NullPointerException("message");
        }

        this.future.reset(session);
        this.message = message;
        this.destination = destination;
    }

    public WriteFutureEx getFuture() {
        return future;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public WriteRequest getOriginalRequest() {
        return this;
    }

    public SocketAddress getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        if (getDestination() == null) {
            return message.toString();
        }

        return message.toString() + " => " + getDestination();
    }

    public static final class ShareableWriteRequest extends ThreadLocal<WriteRequestEx> {
        @Override
        public WriteRequestEx get() {
            WriteRequestEx writeRequest = super.get();

            if (!writeRequest.isResetable()) {
                WriteRequestEx newValue = initialValue();
                set(newValue);
                writeRequest = newValue;
            }

            assert writeRequest.isResetable();
            return writeRequest;
        }

        @Override
        protected WriteRequestEx initialValue() {
            DefaultWriteRequestEx writeRequest = new DefaultWriteRequestEx(new DefaultWriteFutureEx() {
            });
            return writeRequest;
        }

        public static List<ThreadLocal<WriteRequestEx>> initWithLayers(int ioLayers) {
            List<ThreadLocal<WriteRequestEx>> threadLocals = new ArrayList<ThreadLocal<WriteRequestEx>>(ioLayers);
            while (threadLocals.size() < ioLayers) {
                threadLocals.add(new ShareableWriteRequest());
            }
            return unmodifiableList(threadLocals);
        }
    }

}
