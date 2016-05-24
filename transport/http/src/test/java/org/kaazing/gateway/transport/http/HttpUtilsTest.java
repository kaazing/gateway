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
package org.kaazing.gateway.transport.http;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;


public class HttpUtilsTest {

    @Test
    public void getCanonicalURI_lower() throws Exception {
        assertLowerCase(HttpUtils.getCanonicalURI("wss://localhost:8001/echo", true).getHost());
    }

    @Test
    public void getCanonicalURI_mixed() throws Exception {
        URI result = HttpUtils.getCanonicalURI("ws://LocalHost:8001/echo", true);
        assertLowerCase(result.getHost());
        assertEquals("/echo", result.getPath());
    }

    @Test
    public void getCanonicalURI_mixedSSE() throws Exception {
        URI result = HttpUtils.getCanonicalURI("sse://LocalHost:8001/echo", false);
        assertLowerCase(result.getHost());
        assertEquals("/echo", result.getPath());
    }

    @Test
    public void getCanonicalURI_mixedSecureSSE() throws Exception {
        URI result = HttpUtils.getCanonicalURI("sse+ssl://LocalHost:8001/echo", true);
        assertLowerCase(result.getHost());
        assertEquals("/echo", result.getPath());
    }

    @Test
    public void getCanonicalURI_mixedTCP() throws Exception {
        URI result = HttpUtils.getCanonicalURI("tcp://my.Server.com:8001", false);
        assertLowerCase(result.getHost());
        assertEquals("", result.getPath());
    }

    @Test
    public void getCanonicalURI_upper() throws Exception {
        URI result = HttpUtils.getCanonicalURI("wse+ssl://US.KAAZING.COM:8001/my/Path", true);
        assertLowerCase(result.getHost());
        assertEquals("/my/Path", result.getPath());
    }

    @Test
    public void getCanonicalURI_empty() throws Exception {
        assertNull(HttpUtils.getCanonicalURI("", true));
    }

    @Test // Make sure URI without host doesn't cause an NPE
    public void getCanonicalURI_invalid_no_host1() throws Exception {
        URI result = HttpUtils.getCanonicalURI("ws:localhost:8026/", true);
        assertNull(result.getHost());
    }

    @Test // Make sure URI without host doesn't cause an NPE
    public void getCanonicalURI_invalid_no_host2() throws Exception {
        URI result = HttpUtils.getCanonicalURI("ws://:8026", true);
        assertNull(result.getHost());
    }

    @Test
    public void getCanonicalURI_null() throws Exception {
        assertNull(HttpUtils.getCanonicalURI((String)null, false));
    }

    @Test
    public void getCanonicalURI_http_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("http://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_https_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("https://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_tcp_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("tcp://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("", result.getPath());
    }

    @Test
    public void getCanonicalURI_sse_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("sse://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_secure_sse_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("sse+ssl://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_ws_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("ws://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_wss_nopath() throws Exception {
        URI result = HttpUtils.getCanonicalURI("wss://LocalHost:8001", true);
        assertLowerCase(result.getHost());
        assertEquals("/", result.getPath());
    }

    @Test
    public void getCanonicalURI_dontCanonicalizePath_http() throws Exception {
        URI result = HttpUtils.getCanonicalURI("Http://US.KAAZING.COM:8001", false);
        assertLowerCase(result.getHost());
        assertEquals("", result.getPath());
    }

    @Test
    public void getCanonicalURI_dontCanonicalizePath_https() throws Exception {
        URI result = HttpUtils.getCanonicalURI("Http://US.KAAZING.COM:8001", false);
        assertLowerCase(result.getHost());
        assertEquals("", result.getPath());
    }

    @Test
    public void hasCloseHeadersShouldDetectCaseCloseOutOfMany() {
        assertTrue(HttpUtils.hasCloseHeader(Arrays.asList("doodah", "close")));
    }

    @Test
    public void hasCloseHeadersShouldDetectCaseInsensitiveClose() {
        assertTrue(HttpUtils.hasCloseHeader(Collections.singletonList("cLosE")));
    }

    @Test
    public void testRequestWithSpace() {
        HttpRequestMessage request = new HttpRequestMessage();
        request.setRequestURI(URI.create("/Hello%20World.html"));
        IoSession session = new DummySession();
        URI requestURI = HttpUtils.getRequestURI(request, session);
        Assert.assertNotNull(requestURI);
    }

    @Test(expected = URISyntaxException.class)
    public void testMergeQueryParametersThrowsExceptionWhenFromAndIntoURISAreBothNull() throws Exception {
            HttpUtils.mergeQueryParameters(null, null);
    }

    @Test
    public void testNullFromURIGetsMergedCorrectly() throws Exception {
        final String into = "http://www.example.com/";
        final String actual = HttpUtils.mergeQueryParameters(null, into);
        assertEquals(into, actual);
        assertSame(into, actual);
    }

    @Test
    public void testMergeQueryParameters() throws Exception {

        // from has no query param, into has none
        URI from = URI.create("http://from.example.com/path#fragment");
        String into = "http://into.example.com/path#fragment";
        String expected = into;
        verifyMergedURI(from, into, expected);

        // from has one query param, into has none
        from = URI.create("http://from.example.com/path?from1=value1#fragment");
        into = "http://into.example.com/path#fragment";
        expected = "http://into.example.com/path?from1=value1#fragment";
        verifyMergedURI(from, into, expected);

        // from has 2 query param, into has none
        from = URI.create("http://from.example.com/path?from1=value1&from2=value2#fragment");
        into = "http://into.example.com/path#fragment";
        expected = "http://into.example.com/path?from1=value1&from2=value2#fragment";
        verifyMergedURI(from, into, expected);

        // from has no query param, into has one
        from = URI.create("http://from.example.com/path#fragment");
        into = "http://into.example.com/path?in1=value1#fragment";
        expected = "http://into.example.com/path?in1=value1#fragment";
        verifyMergedURI(from, into, expected);

        // from has one query param, into has one, from gets put at the end
        from = URI.create("http://from.example.com/path?from1=value1#fragment");
        into = "http://into.example.com/path?in1=value1#fragment";
        expected = "http://into.example.com/path?in1=value1&from1=value1#fragment";
        verifyMergedURI(from, into, expected);

        // from has just ?
        from = URI.create("http://from.example.com/path?#fragment");
        into = "http://into.example.com/path?in1=value1#fragment";
        expected = "http://into.example.com/path?in1=value1#fragment";
        verifyMergedURI(from, into, expected);

        // from has param= with no value
        from = URI.create("http://from.example.com/path?from1=#fragment");
        into = "http://into.example.com/path?in1=value1#fragment";
        expected = "http://into.example.com/path?in1=value1&from1=#fragment";
        verifyMergedURI(from, into, expected);

        // from has param= with no value
        from = URI.create("http://from.example.com/path?from1=#fragment");
        into = "http://into.example.com/path#fragment";
        expected = "http://into.example.com/path?from1=#fragment";
        verifyMergedURI(from, into, expected);
    }

    private void verifyMergedURI(URI from, String into, String expected) throws URISyntaxException {
        final String actual = HttpUtils.mergeQueryParameters(from, into);
        assertEquals(expected, actual);
        assertNotSame(expected, actual);
    }

    @Test
    public void testIsStreamingScheme() {
        URI uri = null;
        Assert.assertFalse(HttpUtils.hasStreamingScheme(uri));

        uri = URI.create("http://www.example.com");
        Assert.assertFalse(HttpUtils.hasStreamingScheme(uri));

        uri = URI.create("ws://www.example.com");
        Assert.assertFalse(HttpUtils.hasStreamingScheme(uri));

        uri = URI.create("sslwse://www.example.com");
        Assert.assertFalse(HttpUtils.hasStreamingScheme(uri));

        uri = URI.create("tcp://www.example.com");
        Assert.assertTrue(HttpUtils.hasStreamingScheme(uri));

        uri = URI.create("ssl://www.example.com");
        Assert.assertTrue(HttpUtils.hasStreamingScheme(uri));
    }

    public void assertLowerCase(String value) {
        assertTrue(value + " should be lower case", value.equals(value.toLowerCase()));
    }
}
