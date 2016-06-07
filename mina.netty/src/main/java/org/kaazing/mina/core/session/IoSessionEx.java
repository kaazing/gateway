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
package org.kaazing.mina.core.session;

import static org.apache.mina.core.future.DefaultWriteFuture.newWrittenFuture;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import javax.security.auth.Subject;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.service.IoServiceEx;

/**
 * Extended version of IoSession to add support for thread alignment.
*/
public interface IoSessionEx extends IoSession, IoAlignment {

    Thread CURRENT_THREAD = new Thread(new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException("not runnable");
        }

        @Override
        public String toString() {
            return "CURRENT_THREAD";
        }
    });

    Executor IMMEDIATE_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public String toString() {
            return "IMMEDIATE_EXECUTOR";
        }
    };

    Thread NO_THREAD = new Thread(new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException("not runnable");
        }

        @Override
        public String toString() {
            return "NO_THREAD";
        }
    });

    Executor NO_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            throw new IllegalStateException("not executable");
        }

        @Override
        public String toString() {
            return "NO_EXECUTOR";
        }
    };

    WriteRequest REGISTERED_EVENT = new WriteRequest() {

        private final WriteFuture future = newWrittenFuture(null);

        @Override
        public WriteRequest getOriginalRequest() {
            return null;
        }

        @Override
        public Object getMessage() {
            // note: used by strongly typed filters and handlers
            return this;
        }

        @Override
        public WriteFuture getFuture() {
            return future;
        }

        @Override
        public SocketAddress getDestination() {
            return null;
        }
    };

    /**
     * Returns the I/O layer this session represents, with 0 at the base (eg. TCP) and incrementing up through higher layers.
     */
    int getIoLayer();

    /**
     * Returns the IO worker thread in which all filters on the filter chain for this session will be executed
     */
    Thread getIoThread();

    /**
     * Returns an Executor which can be used to execute tasks in the IO worker thread that owns this session.
     * This executor delegates to the executeInIoThread method of the corresponding Netty NioWorker, so when invoked
     * from that same thread, the Executor will immediately execute the task, and when invoked from a different thread,
     * the task will be queued for asynchronous (but quasi-immediate) execution in the worker thread.
     */
    Executor getIoExecutor();

    IoBufferAllocatorEx<?> getBufferAllocator();

    @Override
    IoSessionConfigEx getConfig();

    @Override
    IoServiceEx getService();

    @Override
    boolean isIoAligned();

    void setIoAlignment(Thread ioThread, Executor ioExecutor);

    boolean isIoRegistered();

    /**
     * Returns the Subject representing the current logged on user, or null if none
     */
    Subject getSubject();

    /**
     * Adds a subject change listener to listen for a subject change.
     */
    void addSubjectChangeListener(SubjectChangeListener listener);

    /**
     * Removes a subject change listener if it was previously added.
     */
    void removeSubjectChangeListener(SubjectChangeListener listener);
}
