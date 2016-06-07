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
package org.kaazing.gateway.transport.wseb.connector;

import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsebConnectorIT {

    private final K3poRule robot = new K3poRule();
    private final WsebConnectorRule connector;

    {
        Properties configuration = new Properties();
        configuration.setProperty(WSE_SPECIFICATION.getPropertyName(), "false");
        connector = new WsebConnectorRule(configuration);
    }

    @Rule
    public TestRule chain = createRuleChain(connector, robot);

    @Specification("shouldReplyPongToPing")
    // TODO: remove this once we enable spec test ControlIT
    @Test
    public void shouldReplyPongToPing() throws Exception {
        connector.connect("wse://localhost:8011/path", null, new IoHandlerAdapter<IoSessionEx>() {

        });
        //future.getSession().write(new WsebBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR).wrap(Utils.asByteBuffer("Message from connector")));
        robot.finish();
    }

}
