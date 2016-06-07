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
package org.kaazing.gateway.service.amqp.amqp091.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpCodecFactory implements ProtocolCodecFactory {
    private static final String CLASS_NAME = AmqpCodecFactory.class.getName();

    private ProtocolDecoder  decoder;
    private ProtocolEncoder  encoder;
    private final boolean    client;

    public AmqpCodecFactory(boolean client) {
        Logger logger = LoggerFactory.getLogger("service.amqp.proxy");
        if (logger.isDebugEnabled()) {
            String s = ".AmqpCodecFactory(): client " + client;
            logger.debug(CLASS_NAME + s);
        }
        
        this.client = client;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

        if (session instanceof BridgeSession) {
            BridgeSession bridgeSession = (BridgeSession) session;
            CachingMessageEncoder cachingEncoder = bridgeSession.getMessageEncoder();
            return new AmqpMessageEncoder(cachingEncoder, allocator);
        }

        if (encoder == null) {
            encoder = new AmqpMessageEncoder(allocator);
        }

        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        if (decoder != null) {
            return decoder;
        }
        
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

        decoder = new AmqpMessageDecoder(allocator, client);
        return decoder;
    }
}
