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

package org.kaazing.specification.wse.connector;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class CloseIT {

    private final K3poRule robot = new K3poRule()
    .setScriptRoot("org/kaazing/specification/wse/closing");
    private final WsebConnectorRule connector = new WsebConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connector, robot);



    @Specification("client.send.close/response")
    @Test
    @Ignore // Issue #188: WsebConnector should set Content-Type header on the upstream request
    public void shouldEchoClientCloseFrame() throws Exception {
        connector.connect("wse://localhost:8080//path", null, new IoHandlerAdapter<IoSessionEx>() {

        });

        robot.finish();
    }

}
