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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class DuplicateBindTest {

    @Test(timeout = 10000)
    public void connectingOnService1ShouldNotGetAccessToService2() throws Exception {
        GatewayConfiguration gc = new GatewayConfigurationBuilder().service().name("echo1").type("echo")
                .accept(URI.create("ws://localhost:8000/")).done().service().name("echo2").type("echo")
                .accept(URI.create("ws://localhost:8000/")).done().done();

        Gateway gateway = new Gateway();
        try {
            gateway.start(gc);
        } catch (Exception e) {
            // We do not want an address mapping in the original error binding to address
            assertTrue(
                    "Exception message on binding changed and may not be customer friendly",
                    e.getMessage()
                            .startsWith(
                                    "Error binding to ws://localhost:8000/: Tried to bind address [ws://localhost:8000/ (wse://localhost:8000/)]"));
        }

        try {
            gateway.stop();
        } catch (Exception e) {
            assertFalse(e instanceof java.lang.NullPointerException);
        }
    }
}
