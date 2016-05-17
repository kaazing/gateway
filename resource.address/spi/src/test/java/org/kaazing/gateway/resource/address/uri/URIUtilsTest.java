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
package org.kaazing.gateway.resource.address.uri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.test.util.ResolutionTestUtils;

public class URIUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Test
    public void uriUtilsMethodsBehaviorTcp127001() {
        String uriString = "tcp://127.0.0.1:8080/test?param1=val#fragment";
        assertEquals("tcp", URIUtils.getScheme(uriString));
        asserts127001(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorHttp127001() {
        String uriString = "http://127.0.0.1:8080/test?param1=val#fragment";
        assertEquals("http", URIUtils.getScheme(uriString));
        asserts127001(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackBrackets() {
        String uriString = "tcp://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        assertEquals("[@" + networkInterface + "]", URIUtils.getHost(uriString));
        assertEquals("tcp", URIUtils.getScheme(uriString));
        assertEquals("[@" + networkInterface + "]:8080", URIUtils.getAuthority(uriString));
        generalAsserts(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorUdpLoopbackBrackets() {
        String uriString = "udp://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        assertEquals("[@" + networkInterface + "]", URIUtils.getHost(uriString));
        assertEquals("udp", URIUtils.getScheme(uriString));
        assertEquals("[@" + networkInterface + "]:8080", URIUtils.getAuthority(uriString));
        generalAsserts(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorHttpLoopbackBrackets() {
        String uriString = "http://[@" + networkInterface + "]:8080/test?param1=val#fragment";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Network interface URI syntax should only be applicable for tcp and udp schemes");
        URIUtils.getHost(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorTcpLoopbackNoBrackets() {
        String uriString = "tcp://@" + networkInterface + ":8080/test?param1=val#fragment";
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("Network interface syntax host contains spaces but misses bracket(s)");
        }
        URIUtils.getHost(uriString);
    }

    @Test
    public void uriUtilsMethodsBehaviorUdpLoopbackNoBrackets() {
        String uriString = "udp://@" + networkInterface + ":8080/test?param1=val#fragment";
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("Network interface syntax host contains spaces but misses bracket(s)");
        }
        URIUtils.getHost(uriString);
    }

    /**
     * Method performing asserts on 127.0.0.1
     * @param uri
     */
    private void asserts127001(String uri) {
        assertEquals("127.0.0.1", URIUtils.getHost(uri));
        assertEquals("127.0.0.1:8080", URIUtils.getAuthority(uri));
        generalAsserts(uri);
    }

    /**
     * Method performing general asserts
     * @param uri
     */
    private void generalAsserts(String uri) {
        assertEquals("fragment", URIUtils.getFragment(uri));
        assertEquals("/test", URIUtils.getPath(uri));
        assertEquals("param1=val", URIUtils.getQuery(uri));
        assertEquals(8080, URIUtils.getPort(uri));
        assertNull(URIUtils.getUserInfo(uri));
        assertTrue(URIUtils.resolve(uri, "/a=b").endsWith("/a=b"));
    }

}
