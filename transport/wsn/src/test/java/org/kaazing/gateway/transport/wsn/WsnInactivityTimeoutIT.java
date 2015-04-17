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

package org.kaazing.gateway.transport.wsn;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsnInactivityTimeoutIT {

    private K3poRule robot = new K3poRule();

    private static final boolean ENABLE_DIAGNOSTICS = false;
    @BeforeClass
    public static void init()
            throws Exception {
        if (ENABLE_DIAGNOSTICS) {
            PropertyConfigurator.configure("src/test/resources/log4j-diagnostic.properties");
        }
    }

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept(URI.create("ws://localhost:8001/echo"))
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .acceptOption("ws.inactivity.timeout", "2sec")
                    .done()
                    .service()
                        .accept(URI.create("ws://localhost:80/echo80"))
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .acceptOption("ws.inactivity.timeout", "2sec")
                        .acceptOption("tcp.bind", "8080")
                    .done()
                .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    @Specification("shouldInactivityTimeout")
    @Test(timeout = 8 * 1000) //2s should suffice (twice the expected 2 second timeout), but leave a margin just in case
    public void shouldInactivityTimeout() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtension")
    @Test(timeout = 8 * 1000) //4s should suffice (twice the expected 2 second timeout), but leave a margin just in case
    public void shouldInactivityTimeoutWithPingPongExtension() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshake")
    @Test(timeout = 8 * 1000) //4s should suffice (twice the expected 2 second timeout), but leave a margin just in case
    public void shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshake() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshakePort80")
    @Test(timeout = 8 * 1000) //4s should suffice (twice the expected 2 second timeout), but leave a margin just in case
    // Test case for KG-10384
    // This test originally was required to run against port 80, however that was FUBAR because failsafe doesn't
    // have permission to run against port 80 mac or linux.  So we have used a tcp.bind to 8080 and this was shown
    // to be sufficient to run the test and reproduce the issue
    public void shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshakePort80() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWhenNetworkFailsDuringExtendedHandshake")
    @Test(timeout = 8 * 1000) //4s should suffice (twice the expected 2 second timeout), but leave a margin just in case
    public void shouldInactivityTimeoutWhenNetworkFailsDuringExtendedHandshake() throws Exception {
        robot.finish();
    }
    
}
