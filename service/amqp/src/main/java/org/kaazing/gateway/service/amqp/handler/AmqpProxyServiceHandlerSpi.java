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
package org.kaazing.gateway.service.amqp.handler;

import java.util.Collection;
import java.util.Collections;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.amqp.ProxyServiceHandlerSpi;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpCodecFilter;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;

public class AmqpProxyServiceHandlerSpi extends ProxyServiceHandlerSpi {
    private static final String CLASS_NAME = AmqpProxyServiceHandlerSpi.class.getName();

    @Override
    public void sessionCreated(IoSession ioSession) {
        Logger logger = getServiceContext().getLogger();
        if (logger.isDebugEnabled()) {
            logger.debug("Session created: " + ioSession);
        }

        super.sessionCreated(ioSession);
    }
    
    @Override
    public void sessionClosed(IoSession ioSession) {
        Logger logger = getServiceContext().getLogger();
        if (logger.isDebugEnabled()) {
            logger.debug("Session closed: " + ioSession);
        }
        super.sessionClosed(ioSession);
    }

    @Override
    protected void initFilterChain(IoSession session, boolean client) {
        Logger logger = getServiceContext().getLogger();
        if (logger.isDebugEnabled()) {
            String s = ".initFilterChain()  client = " + client;
            logger.debug(CLASS_NAME + s);
        }

        super.initFilterChain(session, client);

        // Eventually, the protocol will be available as a property of
        // the service. For time being, let's hardcode it to AMQP 0_9_1.
        ProtocolCodecFilter codec = new AmqpCodecFilter(client);
        IoFilterChain filterChain = session.getFilterChain();

        filterChain.addLast(AmqpCodecFilter.NAME, codec);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".initFilterChain(): Add codec filter");
        }
    }

    @Override
    public Collection<String> getProtocols() {
        return Collections.singleton("amqp/0.9.1");
    }
}
