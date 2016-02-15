/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.wseb;

import static org.junit.Assert.assertFalse;

import java.net.URI;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class DuplicateBindTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Gateway gateway;

    @After
    public void tearDown() throws Exception {
        // Shutdown the gateway
        try {
            gateway.stop();
        } catch (Exception e) {
            assertFalse(e instanceof java.lang.NullPointerException);
        }
    }

    @Test(timeout = 10000)
    public void connectingOnService1ShouldNotGetAccessToService2() throws Exception {

        // Configure the gateway
        GatewayConfiguration gc = getGatewayConfiguration();
        gateway = new Gateway();

        // Exception expectations
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(
                "Error binding to ws://localhost:8000/: Tried to bind address [ws://localhost:8000/ (wse://localhost:8000/)]");

        // Startup the gateway
        gateway.start(gc);
    }

    /**
     * Helper method returning a gateway config
     * @return
     */
    private GatewayConfiguration getGatewayConfiguration() {
        // @formatter:off
        GatewayConfiguration gc =
                new GatewayConfigurationBuilder().
                        service().
                            name("echo1").
                            type("echo").
                            accept(URI.create("ws://localhost:8000/")).
                        done().
                        service().
                            name("echo2").
                            type("echo").
                            accept(URI.create("ws://localhost:8000/")).
                        done().
                        done();
        // @formatter:on
        return gc;
    }
}
