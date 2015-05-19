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

package org.kaazing.gateway.transport.sse;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class SseSameOriginIT {
    private TestRule trace = new MethodExecutionTrace();
    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));
    private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                              .accept(URI.create("sse://localhost:8005/sse"))
                              .connect(URI.create("tcp://localhost:7556"))
                              .type("broadcast")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(trace).around(robot).around(gateway).around(timeout);

    @Specification("sse.connect.via.dotnet.emulated")
    @Test
    public void sseEmulatedConnect() throws Exception {
        robot.finish();
    }

    @Specification("sse.connect.via.ie8.httpxe")
    @Test
    public void sseIe8HttpxeConnect() throws Exception {
        robot.finish();
    }

    @Specification("sse.connect.and.get.data.via.ie8.httpxe")
    @Test
    public void sseIe8HttpxeConnectAndGetData() throws Exception {
        robot.finish();
    }
}
