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
package org.kaazing.mina.core.service;

import static org.kaazing.mina.core.util.Util.verifyInIoThread;

import org.kaazing.mina.core.session.IoSessionEx;

/**
 * All of our IoProcessor implementations (in mina.netty and gateway.server) must extend this class, which
 * guarantees all session related methods will be executed on the session's I/O thread (aka thread alignment).
 * If any such method is called in a thread other than the session's I/O thread, it will be scheduled for execution
 * in the I/O thread, otherwise it will be executed immediately.
 * This is similar to what is done in DefaultIoFilterChainEx. The objective is to ensure that all pipeline processing
 * for a given IoSession is done in the same thread.
 */
public abstract class AbstractIoProcessor<T extends IoSessionEx> implements IoProcessorEx<T>  {

    // By definition, dispose() (and isDisposed/ing) must be callable from a non-IO thread,
    // as must processor constructors, since in principle the I/O threads don't even exist
    // until the processor is created and started

    @Override
    public final void add(final T session) {
        // note: alignment is optional before 4.0
        if (session.isIoAligned()) {
            verifyInIoThread(session, session.getIoThread());
        }
        add0(session);
    }
    protected abstract void add0(T session);

    @Override
    public final void flush(final T session) {
        // note: alignment is optional before 4.0
        if (session.isIoAligned()) {
            verifyInIoThread(session, session.getIoThread());
        }
        flush0(session);
    }
    protected abstract void flush0(T session);

    @Override
    public final void remove(final T session) {
        // note: alignment is optional before 4.0
        if (session.isIoAligned()) {
            verifyInIoThread(session, session.getIoThread());
        }
        remove0(session);
    }
    protected abstract void remove0(T session);

    @Override
    public final void updateTrafficControl(final T session) {
        // note: alignment is optional before 4.0
        if (session.isIoAligned()) {
            verifyInIoThread(session, session.getIoThread());
        }
        updateTrafficControl0(session);
    }
    protected abstract void updateTrafficControl0(T session);

}
