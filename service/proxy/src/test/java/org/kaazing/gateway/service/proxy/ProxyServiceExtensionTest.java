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
package org.kaazing.gateway.service.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.test.util.ITUtil;

public class ProxyServiceExtensionTest {

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("tcp://localhost:8888"))
                            .connect(URI.create("tcp://localhost:8051"))
                            .type("proxy")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(gateway, 20, SECONDS);

    @Test
    public void shouldInvokeExtension() throws Exception {
        CountDownLatch backendReady = new CountDownLatch(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket backendServer = new ServerSocket()) {
                    backendServer.bind(new InetSocketAddress("localhost", 8051));
                    backendReady.countDown();
                    Socket s = backendServer.accept();
                    s.close();
                } catch (Exception ex) {
                    fail("Unexpected exception in backend server: " + ex);
                }
            }
        }, "backendServer-thread");
        t.start();

        boolean backendBound = backendReady.await(1, TimeUnit.SECONDS);
        assertTrue("Backend Server failed to bind, pre-conditions not established", backendBound);

        // connect to the service to ensure the extension is executed
        try (Socket clientSocket = new Socket("localhost", 8888)) {
            boolean success = TestExtension.latch.await(5, TimeUnit.SECONDS);
            assertTrue("Failed to execute all phases of proxy service extension", success);
        } catch (Exception ex) {
            fail("Unexpected exception in client connecting to server: " + ex);
        }
    }

}
