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
package org.kaazing.gateway.transport.wsn.specification.extensions.idletimeout;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wsn.WsnAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Class performing x-kaazing-idle-timeout-extension WSN acceptor robot tests integrated with the gateway
 *
 */
public class IdleTimeoutExtensionWSNAcceptorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws.extensions/x-kaazing-idle-timeout");

    private final WsnAcceptorRule acceptor = new WsnAcceptorRule();
    
    private final ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);    

    @Test
    @Specification("standard.pong.frames.sent.by.server.no.client.timeout/request")
    public void standardPongFramesSentByServerNoClientTimeout() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("ws.inactivityTimeout", 2000L);
        ResourceAddress address = addressFactory.newResourceAddress(
                "ws://localhost:8001/echo", options);
        acceptor.bind(address, new IoHandlerAdapter<IoSessionEx>());
        k3po.start();        
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_ONE");
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_TWO");        
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_THREE");
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_FOUR");
        Thread.sleep(700);
        k3po.finish();
    }

    @Test
    @Specification("extension.pong.frames.sent.by.server.no.client.timeout/request")
    public void extensionPongFramesSentByServerNoClientTimeout() throws Exception {
    	Map<String, Object> options = new HashMap<>();
        options.put("ws.inactivityTimeout", 2000L);
        ResourceAddress address = addressFactory.newResourceAddress(
                "ws://localhost:8001/echo", options);
        acceptor.bind(address, new IoHandlerAdapter<IoSessionEx>());
        k3po.start();        
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_ONE");
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_TWO");        
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_THREE");
        Thread.sleep(700);
        k3po.notifyBarrier("SEND_FOUR");
        Thread.sleep(700);
        k3po.finish();
    }
}
