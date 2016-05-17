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
package org.kaazing.gateway.transport.ssl;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class SslSession extends AbstractBridgeSession<SslSession, IoBufferEx> {

    public SslSession(IoServiceEx service, 
                      IoProcessorEx<SslSession> processor, 
                      ResourceAddress localAddress, 
                      ResourceAddress remoteAddress, 
                      IoSessionEx parent) {
        super(service, processor, localAddress, remoteAddress, parent, parent.getBufferAllocator(), Direction.BOTH);
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        return IO_MESSAGE_ENCODER;
    }

}
