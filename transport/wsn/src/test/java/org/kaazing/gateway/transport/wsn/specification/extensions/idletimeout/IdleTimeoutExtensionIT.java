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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * Class performing x-kaazing-idle-timeout-extension robot tests integrated with the gateway
 *
 */
public class IdleTimeoutExtensionIT {

    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/ws.extensions/x-kaazing-idle-timeout");

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept("wsn://localhost:8001/echo")
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .acceptOption("ws.inactivity.timeout", "2sec")
                    .done()
                .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("extension.ping.pong.frames.sent.by.server.no.client.timeout/request")
    @Test
    public void extensionPingPongFramesSentByServerNoClientTimeout() throws Exception {
        robot.finish();
    }

    @Specification("standard.ping.pong.frames.sent.by.server.no.client.timeout/request")
    @Test
    public void standardPingPongFramesSentByServerNoClientTimeout() throws Exception {
        robot.finish();
    }
    
    @Specification("extension.pong.frames.sent.by.server.no.client.timeout/request")
    @Test
    @Ignore("WSNAcceptorRule test")
    public void extensionPongFramesSentByServerNoClientTimeout() throws Exception {
        robot.finish();
    }

    @Specification("standard.pong.frames.sent.by.server.no.client.timeout/request")
    @Test
    @Ignore("WSNAcceptorRule test")
    public void standardPongFramesSentByServerNoClientTimeout() throws Exception {
        robot.finish();
    }

    @Specification("negative.timeout.sent.by.server.client.closes.connection/request")
    @Test
    @Ignore("Client test")
    public void negativeTimeoutSentByServerClientClosesConnection() throws Exception {
        robot.finish();
    }

    @Specification("zero.timeout.sent.by.server.client.closes.connection/request")
    @Test
    @Ignore("Client test")
    public void zeroTimeoutSentByServerClientClosesConnection() throws Exception {
        robot.finish();
    }

    @Specification("no.data.sent.by.server.client.timeout/request")
    @Test
    @Ignore("Client test")
    public void noDataSentByServerClientTimeout() throws Exception {
        robot.finish();
    }

    @Specification("downstream.data.sent.by.server.no.client.timeout/request")
    @Test
    @Ignore("Client test")
    public void downstreamDataSentByServerNoClientTimeout() throws Exception {
        robot.finish();
    }
}
