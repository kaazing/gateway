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
package org.kaazing.gateway.server.util.session;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.kaazing.gateway.server.util.io.IoServiceAdapterEx;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * An {@link IoSession} for non-network-use of the classes that depends on {@link IoSession}. This is similar to Mina's
 * DummySession class except it allows a specific IoProcessor to be used.
 */
public class DummyIoSessionEx extends IoSessionAdapterEx {

    private static final TransportMetadata TRANSPORT_METADATA =
            new DefaultTransportMetadata(
                    "mina", "dummy", false, false,
                    SocketAddress.class, IoSessionConfig.class, Object.class);

    private static final SocketAddress ANONYMOUS_ADDRESS = new SocketAddress() {
        private static final long serialVersionUID = -496112902353454179L;

        @Override
        public String toString() {
            return "?";
        }
    };

    /**
     * Creates a new instance.
     */
    public <T extends IoSessionEx> DummyIoSessionEx(IoProcessorEx<T> processor) {
        this(CURRENT_THREAD, IMMEDIATE_EXECUTOR, processor);
    }

    /**
     * Creates a new instance.
     */
    public <T extends IoSessionEx> DummyIoSessionEx(Thread ioThread, Executor ioExecutor, IoProcessorEx<T> processor) {
        super(ioThread, ioExecutor,
                new IoServiceAdapterEx() {
                    {
                        setHandler(new IoHandlerAdapter());
                    }

                    @Override
                    public TransportMetadata getTransportMetadata() {
                        return TRANSPORT_METADATA;
                    }
                }, processor, new DefaultIoSessionDataStructureFactory());

        setHandler(new IoHandlerAdapter());
        setLocalAddress(ANONYMOUS_ADDRESS);
        setRemoteAddress(ANONYMOUS_ADDRESS);
        setTransportMetadata(TRANSPORT_METADATA);
    }
}
