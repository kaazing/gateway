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

package org.kaazing.gateway.transport.http;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class PersistentConnectionsStore {

    private final ThreadLocal<Map<ResourceAddress, List<IoSessionEx>>> connections;
    private final Logger logger;

    PersistentConnectionsStore(Logger logger) {
        this.connections = new VicariousThreadLocal<>();
        this.logger = logger;
    }

    public void add(ResourceAddress address, IoSessionEx session) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Adding http persistent connection %s", session));
        }
        Map<ResourceAddress, List<IoSessionEx>> addressToSessions = connections.get();
        if (addressToSessions == null) {
            addressToSessions = new HashMap<>();
            connections.set(addressToSessions);
        }
        List<IoSessionEx> sessions = addressToSessions.get(address);
        if (sessions == null) {
            sessions = new ArrayList<>();
            addressToSessions.put(address, sessions);
        }
        sessions.add(session);
    }

    public IoSession remove(ResourceAddress address) {
        Map<ResourceAddress, List<IoSessionEx>> addressToSessions = connections.get();
        if (addressToSessions != null) {
            List<IoSessionEx> sessions = addressToSessions.get(address);
            if (sessions != null && !sessions.isEmpty()) {
                IoSessionEx session = sessions.remove(0);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Reusing http persistent connection %s", session));
                }
                return session;
            }
        }
        return null;
    }

    public void remove(ResourceAddress address, IoSession ioSession) {
        Map<ResourceAddress, List<IoSessionEx>> addressToSessions = connections.get();
        if (addressToSessions != null) {
            List<IoSessionEx> sessions = addressToSessions.get(address);
            if (sessions != null && !sessions.isEmpty()) {
                boolean removed = sessions.remove(ioSession);
                if (removed && logger.isDebugEnabled()) {
                    logger.debug(String.format("Removed http persistent connection %s", ioSession));
                }
            }
        }
    }

}
