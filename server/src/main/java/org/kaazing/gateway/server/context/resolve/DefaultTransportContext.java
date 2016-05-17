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
package org.kaazing.gateway.server.context.resolve;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.context.TransportContext;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.Transport;
import org.slf4j.Logger;

public class DefaultTransportContext implements TransportContext<ResourceAddress> {

    private final String name;
    private final BridgeAcceptor acceptor;
    private final BridgeConnector connector;
    private final Transport transport;
    private static final Logger LOGGER = Launcher.getGatewayStartupLogger();

    public DefaultTransportContext(String name, Transport transport) {
        this.name = name;
        this.transport = transport;
        this.acceptor = transport.getAcceptor();
        this.connector = transport.getConnector();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void bind(ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<?> initializer) {
        if (acceptor != null) {
            try {
                acceptor.bind(address, handler, initializer);
            } catch (RuntimeException re) {
                // Catch this RuntimeException and add a bit more information
                // to its message (cf KG-1462)
                throw new RuntimeException(String.format("Error binding to %s: %s", address.getResource(), re.getMessage()), re);
            }
        }
    }

    @Override
    public final void unbind(ResourceAddress address) {
        if (acceptor != null) {
            acceptor.unbind(address).awaitUninterruptibly();
        }
    }

    @Override
    public void connectInit(ResourceAddress address) {
        if (connector != null) {
            connector.connectInit(address);
        }
    }

    @Override
    public ConnectFuture connect(ResourceAddress address,
                                 IoHandler handler,
                                 IoSessionInitializer<? extends ConnectFuture> initializer) {
        if (connector != null) {
            return connector.connect(address, handler, initializer);
        }
        return null;
    }

    @Override
    public void connectDestroy(ResourceAddress address) {
        if (connector != null) {
            connector.connectDestroy(address);
        }
    }

    @Override
    public void dispose() {
        if (acceptor != null) {
            acceptor.dispose();
        }
        if (connector != null) {
            connector.dispose();
        }
    }

    public BridgeAcceptor getAcceptor() {
        return acceptor;
    }

    public BridgeConnector getConnector() {
        return connector;
    }

    public Transport getTransport() {
        return transport;
    }

}
