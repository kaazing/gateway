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

import java.util.Arrays;

import org.apache.mina.core.future.CompositeIoFuture;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

public class DefaultBindFuture extends DefaultIoFuture implements BindFuture {
    private static final BindFuture SUCCEEDED_FUTURE;

    static {
        SUCCEEDED_FUTURE = new DefaultBindFuture();
        SUCCEEDED_FUTURE.setBound();
    }

    public DefaultBindFuture() {
        super(null);
    }

    public static BindFuture succeededFuture() {
        return SUCCEEDED_FUTURE;
    }

    /** Combine futures in a way that minimizes cost(no object creation) for the common case where
     * both have already been fulfilled.
     */
    public static BindFuture combineFutures(BindFuture future1, BindFuture future2) {
        if (future1 == null || future1.isBound()) {
            return future2;
        }
        else if (future2 == null || future2.isBound()) {
            return future1;
        }
        else {
            return new CompositeBindFuture(Arrays.asList(future1, future2));
        }
    }

    @Override
    public boolean isBound() {
        if (isDone() && getException() == null) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    @Override
    public Throwable getException() {
        return getValueAsThrowable(getValue());
    }

    @Override
    public void setBound() {
        setValue(Boolean.TRUE);
    }

    @Override
    public void setException(Throwable exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }
        setValue(exception);
    }

    @Override
    public IoSession getSession() {
        // There is no session associated with an unbind operation
        throw new UnsupportedOperationException("BindFuture.getSession");
    }

    @Override
    public BindFuture await() throws InterruptedException {
        return (BindFuture) super.await();
    }

    @Override
    public BindFuture awaitUninterruptibly() {
        return (BindFuture) super.awaitUninterruptibly();
    }

    @Override
    public BindFuture addListener(IoFutureListener<?> listener) {
        return (BindFuture) super.addListener(listener);
    }

    @Override
    public BindFuture removeListener(IoFutureListener<?> listener) {
        return (BindFuture) super.removeListener(listener);
    }

    private static Throwable getValueAsThrowable(Object value) {
        if (value != null && value instanceof Throwable) {
            return (Throwable) value;
        }
        else {
            return null;
        }
    }

    private static class CompositeBindFuture extends CompositeIoFuture<BindFuture> implements BindFuture {

        public CompositeBindFuture(Iterable<BindFuture> children) {
            super(children);
        }

        @Override
        public boolean isBound() {
            if (isDone() && getException() == null) {
                return (Boolean) getValue();
            } else {
                return false;
            }
        }

        @Override
        public Throwable getException() {
            return getValueAsThrowable(getValue());
        }

        @Override
        public void setBound() {
            setValue(Boolean.TRUE);
        }

        @Override
        public void setException(Throwable exception) {
            if (exception == null) {
                throw new NullPointerException("exception");
            }
            setValue(exception);
        }

    }

}
