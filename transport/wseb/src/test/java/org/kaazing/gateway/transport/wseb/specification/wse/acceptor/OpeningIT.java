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

package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class OpeningIT {

    private TestRule trace = new MethodExecutionTrace();
    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/opening");
    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private GatewayRule gateway = new GatewayRule() {
        {
         // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("wse://localhost:8080/path"))
                            .type("echo")
                        .done()
                    .done();
            // @formatter:on
            init(configuration, "log4j-trace.properties");
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(timeout).around(k3po).around(gateway);

    @Ignore("Server is not spec compliant")
    @Test
    @Specification("connection.established/handshake.request")
    public void shouldEstablishConnection() throws Exception {
        k3po.finish();
    }

    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.with.body/handshake.request")
    public void shouldEstablishConnectionWithNonEmptyRequestBody() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.method.not.post/handshake.request")
    public void shouldFailHandshakeWhenRequestMethodNotPost() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.sequence.number.missing/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsMissing() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.sequence.number.negative/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNegative() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.sequence.number.non.integer/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNotInteger() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.sequence.number.out.of.range/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsOutOfRange() throws Exception {
        k3po.finish();
    }
    
    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.websocket.version.missing/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionMissing() throws Exception {
        k3po.finish();
    }

    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.websocket.version.not.wseb-1.0/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionNotWseb10()
            throws Exception {
        k3po.finish();
    }

    @Ignore("Server is not spec compliant")
    @Test
    @Specification("request.header.x.accept.commands.not.ping/handshake.request")
    public void shouldFailHandshakeWhenHeaderXAcceptCommandsNotPing() throws Exception {
        k3po.finish();
    }
}
