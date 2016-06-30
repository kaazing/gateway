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
package org.kaazing.mina.core.future;

import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link WriteFuture}.
 */
public class DefaultWriteFutureEx extends DefaultIoFutureEx implements WriteFutureEx {
    /**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'written'.
     */
    public static WriteFutureEx newWrittenFuture(IoSession session) {
        DefaultWriteFutureEx unwrittenFuture = new DefaultWriteFutureEx(session);
        unwrittenFuture.setWritten();
        return unwrittenFuture;
    }

    /**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'not written'.
     */
    public static WriteFutureEx newNotWrittenFuture(IoSession session, Throwable cause) {
        DefaultWriteFutureEx unwrittenFuture = new DefaultWriteFutureEx(session);
        unwrittenFuture.setException(cause);
        return unwrittenFuture;
    }

    /**
     * Creates a new instance.
     */
    public DefaultWriteFutureEx(IoSession session) {
        super(session);
    }

    // "reset" constructor
    protected DefaultWriteFutureEx() {
        super(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritten() {
        if (isDone()) {
            Object v = getValue();
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getException() {
        if (isDone()) {
            Object v = getValue();
            if (v instanceof Throwable) {
                return (Throwable) v;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWritten() {
        setValue(Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setException(Throwable exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }

        setValue(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFutureEx await() throws InterruptedException {
        return (WriteFutureEx) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFutureEx awaitUninterruptibly() {
        return (WriteFutureEx) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFutureEx addListener(IoFutureListener<?> listener) {
        return (WriteFutureEx) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFutureEx removeListener(IoFutureListener<?> listener) {
        return (WriteFutureEx) super.removeListener(listener);
    }
}
