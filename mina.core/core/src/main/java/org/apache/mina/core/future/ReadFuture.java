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

import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoFuture} for {@link IoSession#read() asynchronous read requests}. 
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * // useReadOperation must be enabled to use read operation.
 * session.getConfig().setUseReadOperation(true);
 * 
 * ReadFuture future = session.read();
 * // Wait until a message is received.
 * future.await();
 * try {
 *     Object message = future.getMessage();
 * } catch (Exception e) {
 *     ...
 * }
 * </pre>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ReadFuture extends IoFuture {
    
    /**
     * Returns the received message.  It returns <tt>null</tt> if this
     * future is not ready or the associated {@link IoSession} has been closed. 
     * 
     * @throws RuntimeException if read or any relevant operation has failed.
     */
    Object getMessage();
    
    /**
     * Returns <tt>true</tt> if a message was received successfully.
     */
    boolean isRead();
    
    /**
     * Returns <tt>true</tt> if the {@link IoSession} associated with this
     * future has been closed.
     */
    boolean isClosed();
    
    /**
     * Returns the cause of the read failure if and only if the read
     * operation has failed due to an {@link Exception}.  Otherwise,
     * <tt>null</tt> is returned.
     */
    Throwable getException();

    /**
     * Sets the message is written, and notifies all threads waiting for
     * this future.  This method is invoked by MINA internally.  Please do
     * not call this method directly.
     */
    void setRead(Object message);
    
    /**
     * Sets the associated {@link IoSession} is closed.  This method is invoked
     * by MINA internally.  Please do not call this method directly.
     */
    void setClosed();
    
    /**
     * Sets the cause of the read failure, and notifies all threads waiting
     * for this future.  This method is invoked by MINA internally.  Please
     * do not call this method directly.
     */
    void setException(Throwable cause);

    ReadFuture await() throws InterruptedException;

    ReadFuture awaitUninterruptibly();

    ReadFuture addListener(IoFutureListener<?> listener);

    ReadFuture removeListener(IoFutureListener<?> listener);
}
