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
package org.kaazing.mina.netty.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.ChannelIoSession;
import org.kaazing.mina.netty.socket.DatagramChannelIoConnector;
import org.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;

public class NioDatagramChannelIoConnector extends DatagramChannelIoConnector {

    private static final TransportMetadata NIO_DATAGRAM_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioDatagramChannel", true, true, InetSocketAddress.class,
            DatagramChannelIoSessionConfig.class, Object.class);

    public NioDatagramChannelIoConnector(DatagramChannelIoSessionConfig sessionConfig, DatagramChannelFactory channelFactory) {
        super(sessionConfig, channelFactory);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NIO_DATAGRAM_TRANSPORT_METADATA;
    }

    @Override
    protected ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor) {
        return new NioDatagramChannelIoSession(this, processor, (NioDatagramChannel) channel);
    }

}
