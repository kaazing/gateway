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
package org.kaazing.gateway.server.config.parse.translate.mar2015;

import java.util.Iterator;
import java.util.ListIterator;

import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class ReorderAcceptAndConnectOptionsVisitor extends AbstractVisitor {

    // Root elements in which reordering will occur
    private static final String ACCEPT_OPTIONS = "accept-options";
    private static final String CONNECT_OPTIONS = "connect-options";

    // Constants for the accept-options and connect-options element names
    private static final String HTTP_BIND = "http.bind";
    private static final String HTTP_KEEPALIVE_TIMEOUT = "http.keepalive.timeout";
    private static final String HTTP_TRANSPORT = "http.transport";
    private static final String HTTPS_BIND = "https.bind";
    private static final String PIPE_TRANSPORT = "pipe.transport";
    private static final String SSL_BIND = "ssl.bind";
    private static final String SSL_CIPHERS = "ssl.ciphers";
    private static final String SSL_ENCRYPTION = "ssl.encryption";
    private static final String SSL_PROTOCOLS = "ssl.protocols";
    private static final String SSL_TRANSPORT = "ssl.transport";
    private static final String SSL_VERIFY_CLIENT = "ssl.verify-client";
    private static final String TCP_BIND = "tcp.bind";
    private static final String TCP_MAXIMUM_OUTBOUND_RATE = "tcp.maximum.outbound.rate";
    private static final String TCP_TRANSPORT = "tcp.transport";
    private static final String UDP_INTERFACE = "udp.interface";
    private static final String WS_BIND = "ws.bind";
    private static final String WS_MAXIMUM_MESSAGE_SIZE = "ws.maximum.message.size";
    private static final String WS_INACTIVITY_TIMEOUT = "ws.inactivity.timeout";
    private static final String WS_VERSION = "ws.version";
    private static final String WSS_BIND = "wss.bind";

    @Override
    public void visit(Element element) throws Exception {
        // Visit all <accept-options> elements, ensuring ordering...
        ElementFilter filter = new ElementFilter(ACCEPT_OPTIONS);
        Iterator acceptOptionsIter = element.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(acceptOptionsIter);

        // ... and visit each one.
        while (listIter.hasNext()) {
            Element acceptOptions = listIter.next();
            reorderAcceptOptions(acceptOptions);
        }

        // Visit all <connect-options> elements, ensuring ordering...
        filter = new ElementFilter(CONNECT_OPTIONS);
        Iterator connectOptionsIter = element.getDescendants(filter);
        listIter = toListIterator(connectOptionsIter);

        // ... and visit each one.
        while (listIter.hasNext()) {
            Element connectOptions = listIter.next();
            reorderConnectOptions(connectOptions);
        }
    }

    private void reorderAcceptOptions(Element acceptOptions) {
        int optionsIndex = 0;
        optionsIndex = reorderElement(acceptOptions, SSL_CIPHERS, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, SSL_PROTOCOLS, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, SSL_ENCRYPTION, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, SSL_VERIFY_CLIENT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, WS_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, WSS_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, HTTP_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, HTTPS_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, SSL_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, TCP_BIND, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, WS_MAXIMUM_MESSAGE_SIZE, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, WS_INACTIVITY_TIMEOUT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, HTTP_KEEPALIVE_TIMEOUT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, PIPE_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, TCP_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, SSL_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, HTTP_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, TCP_MAXIMUM_OUTBOUND_RATE, optionsIndex);
        optionsIndex = reorderElement(acceptOptions, UDP_INTERFACE, optionsIndex);
    }

    private void reorderConnectOptions(Element connectOptions) {
        int optionsIndex = 0;
        optionsIndex = reorderElement(connectOptions, SSL_CIPHERS, optionsIndex);
        optionsIndex = reorderElement(connectOptions, SSL_PROTOCOLS, optionsIndex);
        optionsIndex = reorderElement(connectOptions, SSL_ENCRYPTION, optionsIndex);
        optionsIndex = reorderElement(connectOptions, TCP_BIND, optionsIndex);
        optionsIndex = reorderElement(connectOptions, WS_VERSION, optionsIndex);
        optionsIndex = reorderElement(connectOptions, PIPE_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(connectOptions, TCP_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(connectOptions, SSL_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(connectOptions, HTTP_TRANSPORT, optionsIndex);
        optionsIndex = reorderElement(connectOptions, WS_INACTIVITY_TIMEOUT, optionsIndex);
        optionsIndex = reorderElement(connectOptions, UDP_INTERFACE, optionsIndex);
    }

    private int reorderElement(final Element parent,
                               final String name,
                               int newIdx) {
        Element elt = parent.getChild(name, parent.getNamespace());
        if (elt != null) {
            parent.removeContent(elt);
            parent.addContent(newIdx, elt);
            newIdx++;
        }

        return newIdx;
    }
}
