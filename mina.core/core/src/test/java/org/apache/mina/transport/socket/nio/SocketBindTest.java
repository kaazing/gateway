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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.transport.AbstractBindTest;

/**
 * Tests {@link NioSocketAcceptor} resource leakage.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SocketBindTest extends AbstractBindTest {

    public SocketBindTest() {
        super(new NioSocketAcceptor());
    }

    @Override
    protected SocketAddress createSocketAddress(int port) {
        return new InetSocketAddress("localhost", port);
    }

    @Override
    protected int getPort(SocketAddress address) {
        return ((InetSocketAddress) address).getPort();
    }

    @Override
    protected IoConnector newConnector() {
        return new NioSocketConnector();
    }
}
