/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.http.bridge.filter;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.WsHandshakeValidator;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;

public class HttpRequestProfileTest {

    public static final String PREFLIGHT_COOKIES_URI = "ws://localhost:8001/echo/;e/cookies?krn=4324324&.kl=Y";
    public static final String RAW_URI = "http://localhost:8000";
    public static final String NATIVE_WEBSOCKET_URI = "ws://localhost:8001/echo";
    public static final String[] EMULATED_URIS =
            {"ws://localhost:8001/echo/;e/cb", "ws://localhost:8001/echo/;e/ub","ws://localhost:8001/echo/;e/db",
             "ws://localhost:8001/echo/;e/ct", "ws://localhost:8001/echo/;e/ut", "ws://localhost:8001/echo/;e/dt",
             "ws://localhost:8001/echo/;e/cte", "ws://localhost:8001/echo/;e/ube", "ws://localhost:8001/echo/;e/dbe"}
            ;

    public static final String[] AUTHORIZE_URIS =
            {"http://localhost:8001/echo/;a", "http://localhost:8001/echo/;ae", "http://localhost:8001/echo/;ar"};

    HttpRequestMessage request;
    

    @BeforeClass
    public static void beforeClass() throws Exception {
        new WsHandshakeValidator().init();
    }
    @Before
    public void initRequest() {
        request = new HttpRequestMessage();
    }
    @Test(expected = NullPointerException.class)
    public void testNullRequest() throws Exception {
        HttpRequestProfile.valueOf((HttpRequestMessage)null);
    }

    @Test
    public void testPreflight() throws Exception {
        request.setRequestURI(URI.create(PREFLIGHT_COOKIES_URI));
        assertEquals(HttpRequestProfile.PREFLIGHT_COOKIES, HttpRequestProfile.valueOf(request));
    }

    @Test
    public void testRawHttp() throws Exception {
        request.setRequestURI(URI.create(RAW_URI));
        assertEquals(HttpRequestProfile.RAW_HTTP, HttpRequestProfile.valueOf(request));
    }

    @Test
    public void testEmulated() throws Exception {
        for ( String uriStr: EMULATED_URIS) {
            request = new HttpRequestMessage();
            request.setRequestURI(URI.create(uriStr));
            assertEquals(uriStr.contains(";e/c")? HttpRequestProfile.EMULATED_WEB_SOCKET_CREATE:
                         HttpRequestProfile.EMULATED_WEB_SOCKET, HttpRequestProfile.valueOf(request));
        }
    }

    @Test
    public void testAuthorized() throws Exception {
        for ( String uriStr: AUTHORIZE_URIS) {
            request = new HttpRequestMessage();
            request.setRequestURI(URI.create(uriStr));
            assertEquals(uriStr.endsWith(";a") ? HttpRequestProfile.WEBSOCKET_REVALIDATE :
                    HttpRequestProfile.EMULATED_WEB_SOCKET_REVALIDATE, HttpRequestProfile.valueOf(request));
        }
    }

    @Test
    public void testProtocolUpgrade() throws Exception {
        request.setRequestURI(URI.create(NATIVE_WEBSOCKET_URI));
        request.addHeader("Connection", "upgrade");
        request.addHeader("Upgrade", "NotWebSocket");
        assertEquals(HttpRequestProfile.OTHER_PROTOCOL_UPGRADE, HttpRequestProfile.valueOf(request));
    }

    @Test
    public void testWebSocketProtocolUpgrade() throws Exception {
        request.setRequestURI(URI.create(NATIVE_WEBSOCKET_URI));
        request.addHeader("Connection", "upgrade");
        request.addHeader("Upgrade", "WebSocket");
        request.addHeader("Host", "localhost");
        request.setVersion(HttpVersion.HTTP_1_1);
        request.setMethod(HttpMethod.GET);
        assertEquals(HttpRequestProfile.WEBSOCKET_UPGRADE, HttpRequestProfile.valueOf(request));
    }
}
