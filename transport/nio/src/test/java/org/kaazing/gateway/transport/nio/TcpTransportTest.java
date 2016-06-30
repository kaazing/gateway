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
package org.kaazing.gateway.transport.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.util.Collection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.nio.internal.TcpExtensionFactory;

public class TcpTransportTest {

    private TcpTransport transport = new TcpTransport(new Properties());

    private ResourceAddress address;

    @Before
    public void before() throws Exception {
        address = newResourceAddressFactory().newResourceAddress("tcp://localhost:8888");
    }

    @Test
    public void getAcceptorShouldReturnAcceptor() throws Exception {
        BridgeAcceptor acceptor = transport.getAcceptor();
        assertTrue(acceptor instanceof NioSocketAcceptor);
    }

    @Test
    public void getAcceptorWithResourceAddressShouldReturnAcceptor() throws Exception {
        BridgeAcceptor acceptor = transport.getAcceptor(address);
        assertTrue(acceptor instanceof NioSocketAcceptor);
    }

    @Test
    public void getConnectorShouldReturnAcceptor() throws Exception {
        BridgeConnector connector = transport.getConnector();
        assertTrue(connector instanceof NioSocketConnector);
    }

    @Test
    public void getConnectorWithResourceAddressShouldReturnAcceptor() throws Exception {
        BridgeConnector connector = transport.getConnector(address);
        assertTrue(connector instanceof NioSocketConnector);
    }

    @Test
    public void getExtensionsShouldReturnAvailableExtensions() {
        Collection<?> extensions = transport.getExtensions();
        assertEquals(extensions, TcpExtensionFactory.newInstance().availableExtensions());
    }

}
