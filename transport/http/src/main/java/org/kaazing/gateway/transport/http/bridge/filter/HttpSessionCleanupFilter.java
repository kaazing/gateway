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
package org.kaazing.gateway.transport.http.bridge.filter;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

/**
 * If we are handling a request for a directory service,
 * or a handshake "create" request for a WebSocket (native
 * or emulated), make sure to wipe out any and all attributes
 * from the TCP session, which may in fact be reused from a
 * previous WebSocket (browsers are free to keep TCP connections alive).
 */
public class HttpSessionCleanupFilter extends HttpBaseSecurityFilter {

    public HttpSessionCleanupFilter() {
        super();
    }

    public HttpSessionCleanupFilter(Logger logger) {
        super(logger);
    }

    @Override
    public void doMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        if (! httpRequestMessageReceived(nextFilter, session, message)) return;

        cleanup(session);

        super.doMessageReceived(nextFilter, session, message);
    }


    public static void cleanup(IoSession session) {
        HttpPersistenceFilter.cleanup(session);
    }

}
