/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.amqp.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.amqp.AmqpProxyServiceExtensionSpi;
import org.kaazing.gateway.service.amqp.ProxyServiceHandlerSpi;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpCodecFilter;
import org.kaazing.gateway.service.proxy.ProxyServiceExtensionSpi;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;

public class AmqpProxyServiceHandlerSpi extends ProxyServiceHandlerSpi {
    private static final String CLASS_NAME = AmqpProxyServiceHandlerSpi.class.getName();
    private final List<AmqpProxyServiceExtensionSpi> extensions;

    public AmqpProxyServiceHandlerSpi() {
        super();
        extensions = new ArrayList<AmqpProxyServiceExtensionSpi>();
    }

    public void registerExtension(AmqpProxyServiceExtensionSpi extension) {
        assert extension != null;
        extensions.add(extension);
    }

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
        IoFilterChain       filterChain = session.getFilterChain();

        filterChain.addLast(AmqpCodecFilter.NAME, codec);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".initFilterChain(): Add codec filter");
        }

        // hook in any AmqpProxyServiceExtensions here and give them a chance
        // to initialize the accept side of the proxy connection (the Gateway
        // acting as a client is the connect side, so !client is the accept side).
        if (!client) {
            for (AmqpProxyServiceExtensionSpi extension : extensions) {
                extension.initAcceptSession(session);
            }
        }
    }

    @Override
    public Collection<String> getProtocols() {
        return Collections.singleton("amqp/0.9.1");
    }
}
