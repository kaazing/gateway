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
package org.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import org.kaazing.mina.netty.ChannelIoAcceptor;
import org.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class DatagramChannelIoAcceptor
    extends ChannelIoAcceptor<DatagramChannelIoSessionConfig, DatagramChannelFactory, InetSocketAddress>
    implements DatagramAcceptor {

    private static final TransportMetadata CONNECTIONLESS_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "DatagramChannel", true, true, InetSocketAddress.class,
            DatagramChannelIoSessionConfig.class, Object.class);

    private IoSessionRecycler sessionRecycler;  // TODO

    protected DatagramChannelIoAcceptor(DatagramChannelIoSessionConfig sessionConfig,
            DatagramChannelFactory channelFactory, ChannelHandler bindHandler) {
        super(sessionConfig, channelFactory, bindHandler, ServerBootstrapFactory.CONNECTIONLESS);
    }

    @Override
    public IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    @Override
    public void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        this.sessionRecycler = sessionRecycler;
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return CONNECTIONLESS_TRANSPORT_METADATA;
    }
}
