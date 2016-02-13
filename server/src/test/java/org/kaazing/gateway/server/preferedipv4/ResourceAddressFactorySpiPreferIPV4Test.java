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
package org.kaazing.gateway.server.preferedipv4;

import static org.junit.Assert.assertFalse;

import java.net.URI;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

/**
 * Class for testing preferIPv4Stack behavior
 */
public class ResourceAddressFactorySpiPreferIPV4Test {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String JAVA_NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    private Gateway gateway;

    @After
    public void tearDown() throws Exception {
        // shutdown the gateway
        try {
            gateway.stop();
        } catch (Exception e) {
            assertFalse(e instanceof java.lang.NullPointerException);
        }

        // cleanup the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");
    }

    @Test
    public void failsStartupWhenMatchingIPv6AddressesNotFound() throws Exception {
        // set the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "true");

        // configure the gateway
        GatewayConfiguration gc = getGatewayConfig();
        gateway = new Gateway();

        // Exception expectations
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No addresses available for binding for URI");

        // startup the gateway
        gateway.start(gc);
    }

    @Test
    public void startupWhenMatchingIPv6AddressesFound() throws Exception {
        // set the IPV4 flag to false
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");

        // startup the gateway
        GatewayConfiguration gc = getGatewayConfig();
        gateway = new Gateway();
        gateway.start(gc);
    }

    /**
    * Helper method returning a gateway config
    * 
    * @return
    */
    private GatewayConfiguration getGatewayConfig() {
        // @formatter:off
        GatewayConfiguration gc =
                new GatewayConfigurationBuilder()
                    .service()
                        .name("echo")
                        .type("echo")
                        .accept(URI.create("ws://[::1]:8000/echo/"))
                    .done()
                .done();
        // @formatter:on
        return gc;
    }
}
