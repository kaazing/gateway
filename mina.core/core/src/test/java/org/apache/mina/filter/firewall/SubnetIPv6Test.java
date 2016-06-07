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
package org.apache.mina.filter.firewall;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import junit.framework.Assert;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SubnetIPv6Test extends TestCase {

    // Test Data
    private static final String TEST_V6ADDRESS = "1080:0:0:0:8:800:200C:417A";

    public void testIPv6() throws UnknownHostException {
        InetAddress a = InetAddress.getByName(TEST_V6ADDRESS);
        
        assertTrue(a instanceof Inet6Address);
        try {
            new Subnet(a, 24);
            fail("IPv6 not supported");
        } catch(IllegalArgumentException e) {
            // signifies a successful test execution
            Assert.assertTrue(true);
        }
    }
}
