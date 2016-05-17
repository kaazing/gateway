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
package org.kaazing.gateway.server.util.io;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

public class IoProcessorAdapter<T extends IoSession> implements IoProcessor<T> {

    private AtomicBoolean disposed;

    public IoProcessorAdapter() {
        disposed = new AtomicBoolean();
    }

    @Override
    public void add(T session) {
    }

    @Override
    public void dispose() {
        disposed.set(true);
    }

    @Override
    public void flush(T session) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        IoFilterChain filterChain = session.getFilterChain();

        while (true) {
            WriteRequest writeRequest = writeRequestQueue.poll(session);
            if (writeRequest == null) {
                break;
            }

            flushInternal(session, writeRequest);

            filterChain.fireMessageSent(writeRequest);
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    @Override
    public boolean isDisposing() {
        return disposed.get();
    }

    @Override
    public void remove(T session) {

    }

    @Override
    public void updateTrafficControl(T session) {

    }

    protected void flushInternal(T session, WriteRequest writeRequest) {
    }

}
