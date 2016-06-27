/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsn.extensions.pingpong;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

//Note: further testing of this extension is done in WsnInactivityTimeoutIT
public class PingPongExtensionIT {

    private final K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept(URI.create("wsn://localhost:8001/echo"))
                        .type("echo")

                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("pingPongExtensionShouldNotEscapeBinary")
    @Test
    public void pingPongExtensionShouldNotEscapeBinary() throws Exception {
        robot.finish();
    }

    @Specification("pingPongExtensionShouldEscapeText")
    @Test
    public void pingPongExtensionShouldEscapeText() throws Exception {
        robot.finish();
    }

    @Specification("shouldNotEchoEscapeFrame")
    @Test
    public void shouldNotEchoEscapeFrame() throws Exception {
        robot.finish();
    }

    @Specification("shouldReplyToExtendedPingWithExtendedPong")
    @Test
    public void shouldReplyToExtendedPingWithExtendedPong() throws Exception {
        robot.finish();
    }

    @Specification("shouldEchoEscapedFrameWithPingPongControlBytes")
    @Test
    public void shouldEchoEscapedFrameWithPingPongControlBytes() throws Exception {
        robot.finish();
    }

}
