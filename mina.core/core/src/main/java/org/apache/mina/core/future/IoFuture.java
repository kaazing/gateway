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
package org.apache.mina.core.future;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IoSession;

/**
 * Represents the completion of an asynchronous I/O operation on an 
 * {@link IoSession}.
 * Can be listened for completion using a {@link IoFutureListener}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFuture {
    /**
     * Returns the {@link IoSession} which is associated with this future.
     */
    IoSession getSession();

    /**
     * Wait for the asynchronous operation to complete.
     * The attached listeners will be notified when the operation is 
     * completed.
     */
    IoFuture await() throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete uninterruptibly.
     * The attached listeners will be notified when the operation is 
     * completed.
     * 
     * @return the current IoFuture
     */
    IoFuture awaitUninterruptibly();

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     *
     * @return <tt>true</tt> if the operation is finished.
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * @deprecated Replaced with {@link #awaitUninterruptibly()}.
     */
    @Deprecated
    void join();

    /**
     * @deprecated Replaced with {@link #awaitUninterruptibly(long)}.
     */
    @Deprecated
    boolean join(long timeoutMillis);

    /**
     * Returns if the asynchronous operation is completed.
     */
    boolean isDone();

    /**
     * Adds an event <tt>listener</tt> which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     */
    IoFuture addListener(IoFutureListener<?> listener);

    /**
     * Removes an existing event <tt>listener</tt> so it won't be notified when
     * the future is completed.
     */
    IoFuture removeListener(IoFutureListener<?> listener);
}
