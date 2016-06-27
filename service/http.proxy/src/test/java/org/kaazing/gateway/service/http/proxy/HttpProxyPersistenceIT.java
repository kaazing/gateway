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

package org.kaazing.gateway.service.http.proxy;

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

public class HttpProxyPersistenceIT {

    private final K3poRule k3po = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("http://localhost:8110"))
                            .connect(URI.create("http://localhost:8080"))
                            .type("http.proxy")
                            .connectOption("http.keepalive.timeout", "5")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };


    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification( "http.proxy.connect.persistence")
    public void connectPersistence() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification( "http.proxy.connect.persistence.timeout")
    public void connectPersistenceTimeout() throws Exception {
        k3po.finish();
    }
}
