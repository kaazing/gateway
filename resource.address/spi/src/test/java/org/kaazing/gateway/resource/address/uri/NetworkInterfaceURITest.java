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
package org.kaazing.gateway.resource.address.uri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.resource.address.networkinterface.resolution.utils.ResolutionTestUtils;

public class NetworkInterfaceURITest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String networkInterface = "";
    static {
        networkInterface = ResolutionTestUtils.getLoopbackInterface();
    }

    @Test
    public void uriUtilsMethodsBehaviorTcp127001() {
        String uriString = "tcp://127.0.0.1:8080/test?param1=val#fragment";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid network interface URI syntax");
        NetworkInterfaceURI.create(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackBrackets() {
        String uriString = "tcp://[@" + networkInterface +
                "]:8080/test?param1=val#fragment";
        NetworkInterfaceURI uri = NetworkInterfaceURI.create(uriString);
        assertEquals("[@" + networkInterface + "]", uri.getHost());
        assertEquals("tcp", uri.getScheme());
        assertEquals("[@" + networkInterface + "]:8080", uri.getAuthority());
        generalAsserts(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorHttpLoopbackBrackets() {
        String uriString = "http://[@" + networkInterface +
                "]:8080/test?param1=val#fragment";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Network interface URI syntax should onlybe applicable for tcp and udp schemes");
        NetworkInterfaceURI.create(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackNoBrackets() {
        String uriString = "tcp://@" + networkInterface +
                ":8080/test?param1=val#fragment";
        NetworkInterfaceURI uri = NetworkInterfaceURI.create(uriString);
        assertEquals("@" + networkInterface, uri.getHost());
        assertEquals("tcp", uri.getScheme());
        assertEquals("@" + networkInterface + ":8080", uri.getAuthority());
        generalAsserts(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorUdpLoopbackNoBrackets() {
        String uriString = "http://@" + networkInterface +
                ":8080/test?param1=val#fragment";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Network interface URI syntax should onlybe applicable for tcp and udp schemes");
        NetworkInterfaceURI.create(uriString);
    }

    /**
     * Method performing general asserts
     * @param uri
     */
    private void generalAsserts(NetworkInterfaceURI uri) {
        assertEquals("fragment", uri.getFragment());
        assertEquals("/test", uri.getPath());
        assertEquals("param1=val", uri.getQuery());
        assertEquals(8080, uri.getPort());
        assertNull(uri.getUserInfo());
    }

}
