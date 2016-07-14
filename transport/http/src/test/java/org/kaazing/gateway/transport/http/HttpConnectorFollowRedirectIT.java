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
package org.kaazing.gateway.transport.http;

import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.SocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
public class HttpConnectorFollowRedirectIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();
    private final K3poRule k3po = new K3poRule();
    @Rule
    public TestRule chain = createRuleChain(connector, k3po);
    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
    }

    @Test
    @Specification("should.receive.redirect.response")
    public void responseMustBeARedirect() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        connector.getConnectOptions().put("http.maximum.redirects", 1);
        ConnectFuture connectFuture = connector.connect("http://localhost:8080/jms", handler,
                new ConnectSessionInitializer());
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        DefaultHttpSession session = (DefaultHttpSession) connectFuture.getSession();
        SocketAddress localAddressPriorToReconnect = session.getLocalAddress();

        k3po.finish();

        SocketAddress localAddressAfterReconnect = session.getLocalAddress();
        Assert.assertNotEquals(localAddressPriorToReconnect, localAddressAfterReconnect);
        ResourceAddress remoteAddress = (ResourceAddress) session.getRemoteAddress();
        Assert.assertEquals(8081, remoteAddress.getResource().getPort());
        Assert.assertTrue(session.getUpgradeFuture().getSession().getRemoteAddress().toString().endsWith("8081"));
    }

    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader("Upgrade", "websocket");
            connectSession.addWriteHeader("Sec-WebSocket-Version" , "13");
            connectSession.addWriteHeader("Sec-WebSocket-Key" , "dGhlIHNhbXBsZSBub25jZQ==");
            connectSession.addWriteHeader("Connection", "Upgrade");
        }
    }

}
