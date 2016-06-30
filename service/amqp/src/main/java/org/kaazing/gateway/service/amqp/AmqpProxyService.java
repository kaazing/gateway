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
package org.kaazing.gateway.service.amqp;

import java.util.ServiceLoader;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.ProxyService;
import org.kaazing.gateway.service.proxy.ProxyServiceHandler;
import org.slf4j.Logger;

public class AmqpProxyService extends ProxyService {
    private Logger logger;

    public AmqpProxyService() {
    }

    @Override
    public void init(ServiceContext context) throws Exception {
        logger = context.getLogger();
        logger.trace("Initializing AMQP Proxy service");
        super.init(context);
    }

    @Override
    public String getType() {
        return "amqp.proxy";
    }

    @Override
    protected ProxyServiceHandler createHandler() {
        // return new ProxyServiceHandler();
        ServiceLoader<ProxyServiceHandlerSpi> handlers = ServiceLoader.load(ProxyServiceHandlerSpi.class);
        for (ProxyServiceHandlerSpi handler : handlers) {
            if (handler.getProtocols().contains("amqp/0.9.1")) {
                if (logger.isDebugEnabled()) {
                    logger.trace("ProxyServiceHandlerSpi for amqp/0.9.1 found");
                }
                return handler;
            }
        }

        throw new IllegalStateException("ProxyServiceHandlerSpi not found");
    }
}
