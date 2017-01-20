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

import static org.junit.Assert.assertEquals;

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class NioSocketChannelIoAcceptorTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void transportNameShouldBeTcp() throws Exception {
        final NioSocketChannelConfig socketConfig = context.mock(NioSocketChannelConfig.class, "socketConfig");
        context.checking(new Expectations() {
            {
                allowing(socketConfig).setOption(with(any(String.class)), with(any(int.class)));
                allowing(socketConfig).setOption(with(any(String.class)), with(any(boolean.class)));
                allowing(socketConfig).setReuseAddress(with(any(boolean.class)));
            }
        });

        NioSocketChannelIoSessionConfig sessionConfig = new NioSocketChannelIoSessionConfig(socketConfig);
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory();
        NioSocketChannelIoAcceptor acceptor = new NioSocketChannelIoAcceptor(sessionConfig, serverChannelFactory);
        assertEquals("tcp", acceptor.getTransportMetadata().getName());
    }


}
