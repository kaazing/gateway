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
package org.kaazing.gateway.transport;

import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;

import java.util.Collection;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.gateway.transport.ObjectLoggingFilter;

public class NextProtocolFilter extends AbstractInboundEventFilter {

    private final Collection<ProtocolDispatcher> dispatchers;

    public NextProtocolFilter(Collection<ProtocolDispatcher> dispatchers) {
        if (dispatchers == null) {
            throw new NullPointerException("dispatchers");
        }
        this.dispatchers = dispatchers;
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        switch (dispatchers.size()) {
        case 0:
            NEXT_PROTOCOL_KEY.set(session, null);
            session.getFilterChain().remove(this);
            break;
        case 1:
            ProtocolDispatcher dispatcher = dispatchers.iterator().next();
            String protocolName = dispatcher.getProtocolName();
            NEXT_PROTOCOL_KEY.set(session, protocolName);
            session.getFilterChain().remove(this);
            break;
        default:
            suspendInboundEvents(nextFilter, session);
            break;
        }

        super.sessionOpened(nextFilter, session);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {

        if (message instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer) message;
            int initialByte = buffer.get(buffer.position()) & 0xff;

            // TODO: consider multiple bytes
            outer:
            for (ProtocolDispatcher dispatcher : dispatchers) {
                Collection<byte[]> discriminators = dispatcher.getDiscriminators();
                for (byte[] discriminator : discriminators) {
                    if (initialByte == discriminator[0]) {
                        String protocolName = dispatcher.getProtocolName();
                        NEXT_PROTOCOL_KEY.set(session, protocolName);
                        break outer;
                    }
                }
            }

            // force NEXT_PROTOCOL_KEY -> null if not detected (avoids stall and eventual out-of-memory)
            flushInboundEvents(nextFilter, session);
            session.getFilterChain().remove(this);
            // If message logging filter is active, allow it to log decoded objects
            ObjectLoggingFilter.moveAfterCodec(session);
        }

        // capture this message if still suspended, or pass through if not
        super.messageReceived(nextFilter, session, message);
    }
}
