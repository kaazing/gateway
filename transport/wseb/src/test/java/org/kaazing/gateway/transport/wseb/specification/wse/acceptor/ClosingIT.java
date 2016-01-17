/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.session.IoSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ClosingIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/closing");

    private WsebAcceptorRule acceptor = new WsebAcceptorRule();

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification("client.send.close/request")
    public void shouldEchoClientCloseFrame() throws Exception {
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>());
        k3po.finish();
    }

    @Test
    @Specification("server.send.close/request")
    public void shouldPerformServerInitiatedClose() throws Exception {
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                session.close(false);
            }

        });
        k3po.finish();
    }

    // This test is only applicable for clients
    // @Specification("server.send.data.after.close/request")
    //public void shouldIgnoreDataFromServerAfterCloseFrame() throws Exception {
    //    k3po.finish();
    //}

    // This test is oonly applicable for clients
    // @Specification("server.send.data.after.reconnect/request")
    // public void shouldIgnoreDataFromServerAfterReconnectFrame()
    //        throws Exception {
    //    k3po.finish();
    //}
}
