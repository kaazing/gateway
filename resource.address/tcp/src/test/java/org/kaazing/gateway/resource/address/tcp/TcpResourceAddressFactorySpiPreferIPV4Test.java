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
package org.kaazing.gateway.resource.address.tcp;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Class for testing preferIPv4Stack behavior.
 */
public class TcpResourceAddressFactorySpiPreferIPV4Test {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String JAVA_NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    private TcpResourceAddressFactorySpi factory = new TcpResourceAddressFactorySpi();
    private String systemPreferedIPv4 = "";

    @Before
    public void before() {
        String initialSystemPrefereIPv4Stack = System.getProperty(JAVA_NET_PREFER_IPV4_STACK);
        if (initialSystemPrefereIPv4Stack != null){
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
                + " No addresses available for binding for URI: tcp://[::1]:8000");

        factory.newResourceAddress("tcp://[::1]:8000");

    }

    @Test
    public void startupWhenMatchingIPv6AddressesFound() throws Exception {
        // set the IPV4 flag to false
        System.setProperty(JAVA_NET_PREFER_IPV4_STACK, "false");

        factory.newResourceAddress("tcp://[::1]:8000");

    }

}
