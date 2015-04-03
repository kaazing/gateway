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

import org.apache.mina.core.session.AttributeKey;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.PersistentConnectionsStore;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.HttpStartMessage;
import org.kaazing.mina.core.session.IoSessionEx;

import java.util.List;

import static org.kaazing.gateway.transport.http.HttpConnector.HTTP_SESSION_KEY;
import static org.kaazing.gateway.transport.http.HttpStatus.*;

/**
 * Manages persistent connections for HttpConnector
 */
public class HttpConnectPersistenceFilter extends HttpFilterAdapter<IoSessionEx> {

    private final ThreadLocal<PersistentConnectionsStore> persistentConnectionsStore;
    private static final AttributeKey CLOSE_OR_UPGRADE = new AttributeKey(HttpConnectPersistenceFilter.class,
            "closeOrUpgrade");

    public HttpConnectPersistenceFilter(ThreadLocal<PersistentConnectionsStore> persistentConnectionsStore) {
        this.persistentConnectionsStore = persistentConnectionsStore;
    }

    @Override
    protected void httpResponseReceived(NextFilter nextFilter, IoSessionEx session,
                HttpResponseMessage httpResponse) throws Exception {
        if (closeHeader(httpResponse) || upgrade(httpResponse)) {
            session.setAttribute(CLOSE_OR_UPGRADE);
        }

        if (httpResponse.isComplete()) {
            reuse(session);
        }
        super.httpResponseReceived(nextFilter, session, httpResponse);
    }

    @Override
    protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session,
                HttpContentMessage httpContent) throws Exception {

        if (httpContent.isComplete()) {
            reuse(session);
        }
        super.httpContentReceived(nextFilter, session, httpContent);
    }

    private void reuse(IoSessionEx session) {
        DefaultHttpSession httpSession = HTTP_SESSION_KEY.get(session);
        // isConnectionClose() true if gateway wants to close connection
        // CLOSE_OR_UPGRADE is true if origin server wants to close connection
        if (!httpSession.isConnectionClose() && !session.containsAttribute(CLOSE_OR_UPGRADE)) {
            session.removeAttribute(CLOSE_OR_UPGRADE);
            persistentConnectionsStore.get().recycle(session);
        }
    }

    /*
     * return true if the origin server sends Connection : close header
     */
    private boolean closeHeader(HttpResponseMessage msg) {
        List<String> connectionValues = msg.getHeaderValues(HttpHeaders.HEADER_CONNECTION, false);
        if (connectionValues != null) {
            for (String connectionValue : connectionValues) {
                if (connectionValue.equalsIgnoreCase("close")) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * return true if the connection is to be upgraded
     */
    private boolean upgrade(HttpResponseMessage httpResponse) {
        return httpResponse.getStatus() == INFO_SWITCHING_PROTOCOLS;
    }

}
