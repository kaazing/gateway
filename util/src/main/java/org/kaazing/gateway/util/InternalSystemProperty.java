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

package org.kaazing.gateway.util;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;

import java.util.Properties;

public enum InternalSystemProperty {

    // TODO: eliminate all use of System.getProperty and pull all used properties into this class
    //       (e.g. in NioSocketAcceptor)

    // transports
    DEBUG_NIOWORKER_POOL
            ("NioWorkerPool.DEBUG"), // true or false

    WS_ENABLED_TRANSPORTS
            ("org.kaazing.gateway.server.transport.ws.ENABLED_TRANSPORTS"),

    WSE_IDLE_TIMEOUT
            ("org.kaazing.gateway.server.transport.wse.IDLE_TIMEOUT", "60"),

    // We are deliberately changing the default that Netty uses (availableProcessors() * 2):
    TCP_PROCESSOR_COUNT
            ("org.kaazing.gateway.server.transport.tcp.PROCESSOR_COUNT", Integer.toString(getRuntime().availableProcessors())),

    // Thread Pool Size for background tasks
    BACKGROUND_TASK_THREADS
            ("org.kaazing.gateway.server.util.scheduler.BACKGROUND_TASK_THREADS",
                    Integer.toString(getRuntime().availableProcessors())),

    // These Buffer sizes were used by Mina. I'm pretty sure they no longer apply.
    TCP_READ_BUFFER_SIZE
            ("org.kaazing.gateway.server.transport.tcp.READ_BUFFER_SIZE"),

    TCP_MINIMUM_READ_BUFFER_SIZE
            ("org.kaazing.gateway.server.transport.tcp.MINIMUM_READ_BUFFER_SIZE"),

    TCP_MAXIMUM_READ_BUFFER_SIZE
            ("org.kaazing.gateway.server.transport.tcp.MAXIMUM_READ_BUFFER_SIZE"),

    TCP_WRITE_TIMEOUT
            ("org.kaazing.gateway.server.transport.tcp.WRITE_TIMEOUT"),

    // Socket Channel Config options (SocketChannelIoSessionConfig)
    TCP_REUSE_ADDRESS
            ("org.kaazing.gateway.server.transport.tcp.REUSE_ADDRESS", "true"),

    TCP_NO_DELAY
            ("org.kaazing.gateway.server.transport.tcp.TCP_NO_DELAY"),

    TCP_BACKLOG
            ("org.kaazing.gateway.server.transport.tcp.BACKLOG"),

    TCP_KEEP_ALIVE
            ("org.kaazing.gateway.server.transport.tcp.KEEP_ALIVE"),

    TCP_RECEIVE_BUFFER_SIZE
            ("org.kaazing.gateway.server.transport.tcp.RECEIVE_BUFFER_SIZE"),

    TCP_SEND_BUFFER_SIZE
            ("org.kaazing.gateway.server.transport.tcp.SEND_BUFFER_SIZE"),

    TCP_SO_LINGER
            ("org.kaazing.gateway.server.transport.tcp.SO_LINGER"),

    TCP_IP_TOS
            ("org.kaazing.gateway.server.transport.tcp.IP_TOS"),

    // services
    BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES
            ("org.kaazing.gateway.server.service.broadcast.MAXIMUM_PENDING_BYTES"),

    BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT
            ("org.kaazing.gateway.server.service.broadcast.DISCONNECT_CLIENTS_ON_RECONNECT"), // true or false

    // management
    MANAGEMENT_SESSION_THRESHOLD
            ("org.kaazing.gateway.management.SESSION_THRESHOLD", "500");

    private final String name;
    private final String defaultValue;

    InternalSystemProperty(String propertyName) {
        this(propertyName, null);
    }

    InternalSystemProperty(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getProperty(Properties configuration) {
        return configuration.getProperty(name, defaultValue);
    }

    public Integer getIntProperty(Properties configuration) {
        String value = getProperty(configuration);
        if (value == null) {
            return null;
        }
        return parseInt(value);
    }

    public String getPropertyName() {
        return name;
    }

    public boolean isSet(Properties configuration) {
        return configuration.containsKey(name);
    }

}
