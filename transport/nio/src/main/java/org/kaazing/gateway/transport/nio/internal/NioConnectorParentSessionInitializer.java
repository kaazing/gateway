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
package org.kaazing.gateway.transport.nio.internal;

import static java.lang.String.format;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class NioConnectorParentSessionInitializer implements IoSessionInitializer<ConnectFuture> {

    private final IoHandler handler;
    private final IoSessionInitializer<IoFuture> initializer;
    private final ResourceAddress connectAddress;
    private final DefaultConnectFuture bridgeConnectFuture;
    private final ResourceAddressFactory addressFactory;
    private final IoProcessorEx<IoSessionAdapterEx> processor;
    private final AtomicReference<IoConnectorEx> connectorReference;

    public NioConnectorParentSessionInitializer(IoHandler handler, IoSessionInitializer<IoFuture> initializer,
                                                ResourceAddress connectAddress, DefaultConnectFuture bridgeConnectFuture, ResourceAddressFactory addressFactory,
                                                IoProcessorEx<IoSessionAdapterEx> processor, AtomicReference<IoConnectorEx> connectorReference) {
        this.handler = handler;
        this.initializer = initializer;
        this.connectAddress = connectAddress;
        this.bridgeConnectFuture = bridgeConnectFuture;
        this.addressFactory = addressFactory;
        this.processor = processor;
        this.connectorReference = connectorReference;
    }

    @Override
    public void initializeSession(final IoSession parent, ConnectFuture future) {
        // TODO why is the ConnectFuture future not used ?
        final IoSessionEx parentEx = (IoSessionEx) parent;

        // initializer for bridge session to specify bridge handler,
        // and call user-defined bridge session initializer if present
        final IoSessionInitializer<IoFuture> bridgeSessionInitializer = new IoSessionInitializer<IoFuture>() {
            @Override
            public void initializeSession(IoSession bridgeSession, IoFuture future) {
                ((IoSessionAdapterEx) bridgeSession).setHandler(handler);
                if (initializer != null) {
                    initializer.initializeSession(bridgeSession, future);
                }
            }
        };
        Callable<IoSessionAdapterEx> tcpBridgeSessionFactory =
            new NioTcpBridgeSessionFactory(parent, parentEx, bridgeSessionInitializer, addressFactory, connectAddress, connectorReference, processor, bridgeConnectFuture);
        parent.setAttribute(AbstractNioConnector.CREATE_SESSION_CALLABLE_KEY, tcpBridgeSessionFactory);
    }

}
