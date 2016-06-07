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
package org.apache.mina.core.session;

import java.net.SocketAddress;

import org.apache.mina.core.service.IoService;

/**
 * A connectionless transport can recycle existing sessions by assigning an
 * {@link IoSessionRecycler} to an {@link IoService}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * TODO More documentation
 */
public interface IoSessionRecycler {
    /**
     * A dummy recycler that doesn't recycle any sessions.  Using this recycler will
     * make all session lifecycle events to be fired for every I/O for all connectionless
     * sessions.
     */
    IoSessionRecycler NOOP = new IoSessionRecycler() {
        public void put(IoSession session) {
            // Do nothing
        }

        public IoSession recycle(SocketAddress localAddress,
                SocketAddress remoteAddress) {
            return null;
        }

        public void remove(IoSession session) {
            // Do nothing
        }
    };

    /**
     * Called when the underlying transport creates or writes a new {@link IoSession}.
     *
     * @param session
     *            the new {@link IoSession}.
     */
    void put(IoSession session);

    /**
     * Attempts to retrieve a recycled {@link IoSession}.
     *
     * @param localAddress
     *            the local socket address of the {@link IoSession} the
     *            transport wants to recycle.
     * @param remoteAddress
     *            the remote socket address of the {@link IoSession} the
     *            transport wants to recycle.
     * @return a recycled {@link IoSession}, or null if one cannot be found.
     */
    IoSession recycle(SocketAddress localAddress, SocketAddress remoteAddress);

    /**
     * Called when an {@link IoSession} is explicitly closed.
     *
     * @param session
     *            the new {@link IoSession}.
     */
    void remove(IoSession session);
}
