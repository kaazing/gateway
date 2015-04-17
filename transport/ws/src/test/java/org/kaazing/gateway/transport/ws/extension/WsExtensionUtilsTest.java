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

package org.kaazing.gateway.transport.ws.extension;

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.extension.WsExtensionUtils.negotiateWebSocketExtensions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpAcceptProcessor;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.session.DummySessionEx;

public class WsExtensionUtilsTest {

    private ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();

    @Test
    public void shouldNegotiatePingPongWsExtension()
        throws Exception {

        String headerName = "Sec-WebSocket-Extensions";

        List<String> requestedExts = new ArrayList<>();
        requestedExts.add("x-kaazing-ping-pong");

        List<String> supportedExts = new ArrayList<>();
        supportedExts.add("x-kaazing-ping-pong");
 
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader("Host", "localhost:8080");

        for (String requestedExt : requestedExts) {
            request.addHeader(headerName, requestedExt);
        }

        URI uri = URI.create("http://localhost:8080/");
        request.setRequestURI(uri);

        ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);
        ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(URI.create("http://localhost:43232/"));
        HttpAcceptor httpAcceptor = new HttpAcceptor();
        HttpAcceptProcessor httpProcessor = new HttpAcceptProcessor();
        DummySessionEx parentSession = new DummySessionEx();
        DefaultHttpSession session = new DefaultHttpSession(httpAcceptor, httpProcessor, address, remoteAddress, parentSession, null, request, uri);

        WsExtensionNegotiationResult wenr = negotiateWebSocketExtensions(null, session, null, requestedExts, supportedExts);
        List<Extension> negotiatedExts = wenr.getExtensions().asList();

        List<Extension> expectedExts = new ArrayList<>();
        ExtensionBuilder web = new ExtensionBuilder("x-kaazing-ping-pong");
        web.appendParameter("01010102");
        expectedExts.add(web.toWsExtension());

        assertTrue(String.format("Expected negotiated WS extensions '%s', got '%s'", expectedExts, negotiatedExts), negotiatedExts.equals(expectedExts));
    }

    @Test
    public void shouldNegotiateEmptyWsExtensions() throws Exception {
        List<String> requestedExts = null;

        List<String> supportedExts = new ArrayList<>();
        supportedExts.add("x-kaazing-foo");
 
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader("Host", "localhost:8080");

        URI uri = URI.create("http://localhost:8080/");
        request.setRequestURI(uri);

        ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);
        ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(URI.create("http://localhost:43232/"));

        HttpAcceptor httpAcceptor = new HttpAcceptor();
        HttpAcceptProcessor httpProcessor = new HttpAcceptProcessor();
        DummySessionEx parentSession = new DummySessionEx();
        DefaultHttpSession session = new DefaultHttpSession(httpAcceptor, httpProcessor, address, remoteAddress, parentSession, null, request, uri);

        WsExtensionNegotiationResult wenr = negotiateWebSocketExtensions(null, session, null, requestedExts, supportedExts);
        List<Extension> negotiatedExts = wenr.getExtensions().asList();

        assertTrue(String.format("Expected no negotiated WS extensions, got '%s'", negotiatedExts), negotiatedExts.size()==0);
    }

    @Test
    public void shouldNegotiateNoCommonWsExtensions() throws Exception {
        List<String> requestedExts = new ArrayList<>();
        requestedExts.add("x-kaazing-foo-bar");

        List<String> supportedExts = new ArrayList<>();
        supportedExts.add("x-kaazing-ping-pong");
        supportedExts.add(null);
 
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader("Host", "localhost:8080");

        URI uri = URI.create("http://localhost:8080/");
        request.setRequestURI(uri);

        ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);
        ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(URI.create("http://localhost:43232/"));

        HttpAcceptor httpAcceptor = new HttpAcceptor();
        HttpAcceptProcessor httpProcessor = new HttpAcceptProcessor();
        DummySessionEx parentSession = new DummySessionEx();
        DefaultHttpSession session = new DefaultHttpSession(httpAcceptor, httpProcessor, address, remoteAddress, parentSession, null, request, uri);

        WsExtensionNegotiationResult wenr = negotiateWebSocketExtensions(null, session, null, requestedExts, supportedExts);
        List<Extension> negotiatedExts = wenr.getExtensions().asList();

        assertTrue(String.format("Expected no negotiated WS extensions, got '%s'", negotiatedExts), negotiatedExts.size()==0);
    }

    @Test
    public void shouldNegotiateOnlyPingPongExtension()
        throws Exception {

        String headerName = "Sec-WebSocket-Extensions";

        List<String> requestedExts = new ArrayList<>();
        requestedExts.add("x-kaazing-ping-pong");

        List<String> supportedExts = new ArrayList<>();
        supportedExts.add("x-kaazing-ping-pong");
        supportedExts.add(null);
 
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader("Host", "localhost:8080");

        for (String requestedExt : requestedExts) {
            request.addHeader(headerName, requestedExt);
        }

        URI uri = URI.create("http://localhost:8080/");
        request.setRequestURI(uri);

        ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);
        ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(URI.create("http://localhost:43232/"));

        HttpAcceptor httpAcceptor = new HttpAcceptor();
        HttpAcceptProcessor httpProcessor = new HttpAcceptProcessor();
        DummySessionEx parentSession = new DummySessionEx();
        DefaultHttpSession session = new DefaultHttpSession(httpAcceptor, httpProcessor, address, remoteAddress, parentSession, null, request, uri);

        WsExtensionNegotiationResult wenr = negotiateWebSocketExtensions(null, session, null, requestedExts, supportedExts);
        List<Extension> negotiatedExts = wenr.getExtensions().asList();

        List<Extension> expectedExts = new ArrayList<>();
        ExtensionBuilder web = new ExtensionBuilder("x-kaazing-ping-pong");
        expectedExts.add(web.toWsExtension());

        assertTrue(String.format("Expected negotiated WS extensions '%s', got '%s'", expectedExts, negotiatedExts), negotiatedExts.equals(expectedExts));
    }
}

