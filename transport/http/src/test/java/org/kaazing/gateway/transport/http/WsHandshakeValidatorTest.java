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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.mina.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;

public class WsHandshakeValidatorTest {

    public static final String SEC_WEB_SOCKET_VERSION = "Sec-WebSocket-Version";
    public static final String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key";
    public static final String SEC_WEB_SOCKET_KEY1 = "Sec-WebSocket-Key1";
    public static final String SEC_WEB_SOCKET_KEY2 = "Sec-WebSocket-Key2";

    public static final String HOST_NAME = "somehost";
    public static final URI REQUEST_URI = URI.create("/echo");
    private WsHandshakeValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new WsHandshakeValidator();
        if (!validator.isInitialized()) {
            assertEquals(0, WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.size());
            validator.init();
            assertEquals(5, WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.size());
            assertNotNull(WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.get(WebSocketWireProtocol.HIXIE_75));
            assertNotNull(WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.get(WebSocketWireProtocol.HIXIE_76));
            assertNotNull(WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.get(WebSocketWireProtocol.HYBI_8));
            assertNotNull(WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.get(WebSocketWireProtocol.HYBI_13));
            assertNotNull(WsHandshakeValidator.handshakeValidatorsByWireProtocolVersion.get(WebSocketWireProtocol.RFC_6455));
            assertTrue(validator.isInitialized());
        }
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testValidateHixie75() throws Exception {

        HttpRequestMessage request = new HttpRequestMessage();
        assertFalse(validator.validate(request));

        assertFalse(validator.validate(request));


        request.setMethod(HttpMethod.GET);
        assertFalse(validator.validate(request));

        request.setVersion(HttpVersion.HTTP_1_1);
        assertFalse(validator.validate(request));

        request.setRequestURI(URI.create("/echo"));
        assertFalse(validator.validate(request));

        request.addHeader("Connection", "Upgrade");
        request.addHeader("Upgrade", "websockeT");
        assertFalse(validator.validate(request));

        request.addHeader("Host", "somehost");
        assertTrue(validator.validate(request));

    }

    @Test
    public void shouldDetectHixie76() throws Exception {

        HttpRequestMessage request = makeHandshakeRequest();

        request.setHeader(SEC_WEB_SOCKET_KEY1, "key");
        request.setHeader(SEC_WEB_SOCKET_KEY2, "key");

        assertTrue(validator.validate(request));
    }

    @Test
    public void shouldDetectHyBi8() throws Exception {

        HttpRequestMessage request = makeHandshakeRequest();

        request.setHeader(SEC_WEB_SOCKET_VERSION, "8");

        assertFalse("We are missing the Sec-WebSocket-Key header.  Should not validate.", validator.validate(request));

        request.addHeader(SEC_WEB_SOCKET_KEY, "InvalidKey");

        assertFalse("We have an invalid websocket key.  Should not validate.", validator.validate(request));

        request = makeHandshakeRequest();
        request.setHeader(SEC_WEB_SOCKET_VERSION, "8");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));

        assertTrue(validator.validate(request));
    }

    @Test
    public void testFailToValidateOnBadVersionNumber() throws Exception {
        HttpRequestMessage request = makeHandshakeRequest();
        request.addHeader(SEC_WEB_SOCKET_VERSION, "Bad");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));
        assertFalse(validator.validate(request));

        request = makeHandshakeRequest();
        request.addHeader(SEC_WEB_SOCKET_VERSION, "500");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));
        assertFalse(validator.validate(request));
    }

    @Test
    public void shouldFailToValidateWithEmptyHostHeader() throws Exception {
        HttpRequestMessage request = makeHandshakeRequest(HttpMethod.GET, HttpVersion.HTTP_1_1, REQUEST_URI, "");
        request.addHeader(SEC_WEB_SOCKET_VERSION, "13");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));
        assertFalse("Should not validate if host header is empty", validator.validate(request));
    }

    

    @Test
    public void shouldDetectHyBi13() throws Exception {

        HttpRequestMessage request = makeHandshakeRequest();

        request.setHeader(SEC_WEB_SOCKET_VERSION, "13");

        assertFalse("We are missing the Sec-WebSocket-Key header.  Should not validate.", validator.validate(request));

        request.addHeader(SEC_WEB_SOCKET_KEY, "InvalidKey");

        assertFalse("We have an invalid websocket key.  Should not validate.", validator.validate(request));

        request = makeHandshakeRequest();
        request.setHeader(SEC_WEB_SOCKET_VERSION, "13");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));

        assertTrue(validator.validate(request));
    }

    private HttpRequestMessage makeHandshakeRequest() {
        return makeHandshakeRequest(HttpMethod.GET,
                HttpVersion.HTTP_1_1, REQUEST_URI, HOST_NAME);
    }


    HttpRequestMessage makeHandshakeRequest(final HttpMethod httpMethod, final HttpVersion httpVersion,
                                            final URI requestURI, final String hostName) {
        HttpRequestMessage request = new HttpRequestMessage();
        request.setMethod(httpMethod);
        request.setVersion(httpVersion);
        request.setRequestURI(requestURI);
        request.addHeader("Connection", "Upgrade");
        request.addHeader("Upgrade", "websockeT");
        request.addHeader("Host", hostName);
        return request;
    }


    @Test
    public void testWebSocketKeyValidation() throws Exception {
        WsProtocol8HandshakeValidator validator = new WsProtocol8HandshakeValidator();
        assertFalse(validator.validateWebSocketKey(null));

        assertFalse(validator.validateWebSocketKey(new String(Base64.encodeBase64(new byte[15]), "UTF-8")));
        assertTrue(validator.validateWebSocketKey(new String(Base64.encodeBase64(new byte[16]), "UTF-8")));
        assertFalse(validator.validateWebSocketKey(new String(Base64.encodeBase64(new byte[17]), "UTF-8")));
    }

    @Test
    public void testHeaderValueCaseInsensitivity() throws Exception {
        final HttpRequestMessage request = makeHandshakeRequest();
        assertTrue(validator.requireHeader(request, "Connection", "Upgrade"));
        assertTrue(validator.requireHeader(request, "Connection", "UpGrAdE"));
    }

    @Test
    public void testRequireHeaderNegativeCases() throws Exception {
        assertFalse(validator.requireHeader(null, null));
        final HttpRequestMessage request = makeHandshakeRequest();
        assertFalse(validator.requireHeader(request, null));
        assertFalse(validator.requireHeader(null, "HeaderName"));

        assertFalse(validator.requireHeader(null, null, null));
        assertFalse(validator.requireHeader(null, "Connection", "Upgrade"));
        assertFalse(validator.requireHeader(request, null, null));
        assertFalse(validator.requireHeader(request, "Connection", null));
        assertFalse(validator.requireHeader(request, null, "Upgrade"));
        assertTrue(validator.requireHeader(request, "Connection", "Upgrade"));
    }

    @Test 
    public void testFailValidationWithPostMethodWhenPostMethodIsNotAllowed() throws Exception {
        final HttpRequestMessage request = makeHandshakeRequest(HttpMethod.POST, HttpVersion.HTTP_1_1, REQUEST_URI, HOST_NAME);
        request.setHeader(SEC_WEB_SOCKET_VERSION, "13");
        request.addHeader(SEC_WEB_SOCKET_KEY, new String(Base64.encodeBase64(new byte[16]), "UTF-8"));

        assertFalse(validator.validate(request, false));
        request.setMethod(HttpMethod.GET);
        assertTrue(validator.validate(request, false));

    }
}

