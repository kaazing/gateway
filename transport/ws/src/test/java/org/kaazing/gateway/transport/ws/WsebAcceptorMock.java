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
package org.kaazing.gateway.transport.ws;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;

public class WsebAcceptorMock extends AbstractBridgeAcceptor {

    public WsebAcceptorMock() {
        super(new DefaultIoSessionConfigEx());
    }

    @Override
    protected Bindings initBindings() {
        return new Bindings.Default();
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("wse") || transportName.equals("ws");
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler ioHandler, BridgeSessionInitializer bridgeSessionInitializer) {
        UnbindFuture future =  new DefaultUnbindFuture();
        future.setUnbound();
        return future;
    }

    @Override
    protected void bindInternal(ResourceAddress address, IoHandler ioHandler, BridgeSessionInitializer bridgeSessionInitializer) {
        ++bindInternalCalls;
    }


    int bindInternalCalls = 0;


    public boolean bindWasCalled() { return bindInternalCalls > 0; }

    public boolean bindWasNotCalled() { return bindInternalCalls == 0; }

    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata("wseb");
    }
}
