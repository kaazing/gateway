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

import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelConfig;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;

class NioDatagramChannelIoSessionConfig extends DatagramChannelIoSessionConfig<NioDatagramChannelConfig>
        implements DatagramSessionConfig {

    NioDatagramChannelIoSessionConfig(NioDatagramChannelConfig channelConfig) {
        super(channelConfig);
    }

    public void init(final IoServiceEx parent) {
        // If we are a SocketAcceptor we always want to flush REUSEADDRESS into the socket.
        if (parent instanceof SocketAcceptor) {
            setReuseAddress(true);
        }
    }

    @Override
    protected final void doSetAll(IoSessionConfigEx config) {

        super.doSetAll(config);

        if (config instanceof NioDatagramChannelIoSessionConfig) {
            channelConfig.setReceiveBufferSizePredictorFactory(
                ((NioDatagramChannelIoSessionConfig) config).channelConfig.getReceiveBufferSizePredictorFactory());
        }
    }
}
