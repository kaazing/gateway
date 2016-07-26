/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.resource.address.udp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.test.util.ResolutionTestUtils;

/**
 * Class for testing preferIPv4Stack behavior.
 */
public class UdpResourceAddressFactorySpiPreferIPV4Test {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static final String JAVA_NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    private String systemPreferedIPv4 = "";
    private UdpResourceAddressFactorySpi factory = new UdpResourceAddressFactorySpi();
    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Before
    public void before() {
        String initialSystemPrefereIPv4Stack = System.getProperty(JAVA_NET_PREFER_IPV4_STACK);
        if (initialSystemPrefereIPv4Stack != null) {
            systemPreferedIPv4 = initialSystemPrefereIPv4Stack;
        }
    }

    @After
    public void tearDown() throws Exception {
        // cleanup the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, systemPreferedIPv4);
    }

    @Test
    public void failsStartupWhenMatchingIPv6AddressesNotFound() throws Exception {
        // set the IPV4 flag
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "true");
        // Exception expectations
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Option java.net.preferIPv4Stack is set to true and an IPv6 address was provided in the config."
                + " No addresses available for binding for URI: udp://[::1]:8000");
        factory.newResourceAddress("udp://[::1]:8000");
    }

    @Test
    public void startupWhenMatchingIPv6AddressesFound() throws Exception {
        // set the IPV4 flag to false
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");
        factory.newResourceAddress("udp://[::1]:8000");
    }

    @Test
    public void shouldNotResolveLoopbackToIPv6IfProhibited() throws Exception {
        // set the IPV4 flag to true
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "true");

        UdpResourceAddress loopbackResourceAddress = factory.newResourceAddress("udp://[@" + networkInterface + "]:8000");

        // assert resource address does not contain IPv6
        assertFalse(loopbackResourceAddress.toString().contains("0:0:0:0:0:0:0:1"));
    }

    @Test
    public void shouldResolveLoopbackToIPv6IfNotProhibited() throws Exception {
        // set the IPV4 flag to false
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");

        UdpResourceAddress loopbackResourceAddress = factory.newResourceAddress("udp://[@" + networkInterface + "]:8000");

        // TravisCI does not support IPV6
        boolean onTravisCI = System.getenv().containsKey("TRAVIS");
        // assert resource address contains IPv6
        if (!onTravisCI) {
            assertTrue(loopbackResourceAddress.toString().contains("0:0:0:0:0:0:0:1"));
        }
    }
}
