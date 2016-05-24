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
package org.kaazing.mina.core.write;

import static java.util.Collections.unmodifiableList;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

/**
 * Extended version of WriteRequest to add support for mutating the
 * message during encoding to avoid undesirable allocation.
 */
public class DefaultWriteRequestEx implements WriteRequestEx {

    private static final WriteFutureEx UNUSED_FUTURE = new WriteFutureEx() {
        @Override
        public boolean isWritten() {
            return false;
        }

        @Override
        public void setWritten() {
            // Do nothing
        }

        @Override
        public IoSession getSession() {
            return null;
        }

        @Override
        public void join() {
            // Do nothing
        }

        @Override
        public boolean join(long timeoutInMillis) {
            return true;
        }

        @Override
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

        @Override
        public WriteFuture addListener(IoFutureListener<?> listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        @Override
        public WriteFuture removeListener(IoFutureListener<?> listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        @Override
        public WriteFuture await() throws InterruptedException {
            return this;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit)
                throws InterruptedException {
            return true;
        }

        @Override
        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        @Override
        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        @Override
        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }

        @Override
        public Throwable getException() {
            return null;
        }

        @Override
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

    @Override
    public WriteFutureEx getFuture() {
        return future;
    }

    @Override
    public Object getMessage() {
        return message;
    }

    @Override
    public void setMessage(Object message) {
        this.message = message;
    }

    @Override
    public WriteRequest getOriginalRequest() {
        return this;
    }

    @Override
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

    public static final class ShareableWriteRequest extends VicariousThreadLocal<WriteRequestEx> {
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
            List<ThreadLocal<WriteRequestEx>> threadLocals = new ArrayList<>(ioLayers);
            while (threadLocals.size() < ioLayers) {
                threadLocals.add(new ShareableWriteRequest());
            }
            return unmodifiableList(threadLocals);
        }
    }

}
