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
package org.kaazing.gateway.transport.wsn;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.wsn.specification.ws.connector.WsnConnectorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
public class WsConnectorFollowRedirectIT {

    private final WsnConnectorRule connector = new WsnConnectorRule();
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
        connector.connect("ws://localhost:8080/jms", null, handler);

        k3po.finish();
    }

}
