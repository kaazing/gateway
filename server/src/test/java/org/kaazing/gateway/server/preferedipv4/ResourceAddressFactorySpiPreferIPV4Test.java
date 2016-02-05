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
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

/**
 * Class for testing preferIPv4Stack behavior
 */
public class ResourceAddressFactorySpiPreferIPV4Test {

    private static final String JAVA_NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";

    @Test
    public void testEmptyCollectionWithFlagTrue() {
        // set the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "true");

        // startup the gateway
        GatewayConfiguration gc = getGatewayConfig();
        Gateway gateway = new Gateway();
        startupTheGatewayWithAssertions(gc, gateway);

        // shutdown the gateway
        try {
            gateway.stop();
        } catch (Exception e) {
            assertFalse(e instanceof java.lang.NullPointerException);
        }

        // cleanup the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");
    }

    /**
    * Helper method returning a gateway config
    * 
    * @return
    */
    private GatewayConfiguration getGatewayConfig() {
        GatewayConfiguration gc = new GatewayConfigurationBuilder().
                service().
                    name("echo").
                        type("echo").
                        accept(URI.create("ws://[::1]:8000/echo/")).
                    done().
                done();
        return gc;
    }

    /**
    * Method for starting up the gateway and ensuring a failure is received
    * according to the current scenario
    * 
    * @param gc
    * @param gateway
    */
    private void startupTheGatewayWithAssertions(GatewayConfiguration gc, Gateway gateway) {
        try {
            gateway.start(gc);
            assertTrue("This should not be reached", false);
        } catch (AssertionError e) {
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Exception message on binding", e.getMessage().startsWith("No addresses available for binding for URI"));
        }
    }
}
