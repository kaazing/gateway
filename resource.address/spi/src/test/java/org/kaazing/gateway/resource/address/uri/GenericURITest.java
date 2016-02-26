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

public class GenericURITest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Test
    public void uriUtilsMethodsBehaviorTcp127001() {
        String uriString = "tcp://127.0.0.1:8080/test?param1=val#fragment";
        GenericURI uri = GenericURI.create(uriString);
        assertEquals("tcp", uri.getScheme());
        asserts127001(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorHttp127001() {
        String uriString = "http://127.0.0.1:8080/test?param1=val#fragment";
        GenericURI uri = GenericURI.create(uriString);
        assertEquals("http", uri.getScheme());
        asserts127001(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackBrackets() {
        String uriString = "tcp://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        GenericURI uri = GenericURI.create(uriString);
        assertEquals("[@" + networkInterface + "]", uri.getHost());
        assertEquals("tcp", uri.getScheme());
        assertEquals("[@" + networkInterface + "]:8080", uri.getAuthority());
        generalAsserts(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorUdpLoopbackBrackets() {
        String uriString = "udp://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        GenericURI uri = GenericURI.create(uriString);
        assertEquals("[@" + networkInterface + "]", uri.getHost());
        assertEquals("udp", uri.getScheme());
        assertEquals("[@" + networkInterface + "]:8080", uri.getAuthority());
        generalAsserts(uri);
    }

    @Test
    public void uriUtilsMethodsBehaviorHttpLoopbackBrackets() {
        String uriString = "http://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Network interface URI syntax should only be applicable for tcp and udp schemes");
        GenericURI.create(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackNoBrackets() {
        String uriString = "tcp://@" + networkInterface + ":8080/test?param1=val#fragment";
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("Network interface syntax host contains spaces but misses bracket(s)");
        }
        GenericURI.create(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorUdpLoopbackNoBrackets() {
        String uriString = "udp://@" + networkInterface + ":8080/test?param1=val#fragment";
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("Network interface syntax host contains spaces but misses bracket(s)");
        }
        GenericURI.create(uriString);
    }

    /**
     * Method performing asserts on 127.0.0.1
     * @param uri
     */
    private void asserts127001(GenericURI uri) {
        assertEquals("127.0.0.1", uri.getHost());
        assertEquals("127.0.0.1:8080", uri.getAuthority());
        generalAsserts(uri);
    }

    /**
     * Method performing general asserts
     * @param uri
     */
    private void generalAsserts(GenericURI uri) {
        assertEquals("fragment", uri.getFragment());
        assertEquals("/test", uri.getPath());
        assertEquals("param1=val", uri.getQuery());
        assertEquals(8080, uri.getPort());
        assertNull(uri.getUserInfo());
    }

}
