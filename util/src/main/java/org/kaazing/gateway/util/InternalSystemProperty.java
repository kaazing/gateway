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
package org.kaazing.gateway.util;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;

import java.util.Properties;

public enum InternalSystemProperty {

    // TODO: eliminate all use of System.getProperty and pull all used properties into this class
    //       (e.g. in NioSocketAcceptor)

    WS_ENABLED_TRANSPORTS
            ("org.kaazing.gateway.server.transport.ws.ENABLED_TRANSPORTS"),

    WS_CLOSE_TIMEOUT
            ("org.kaazing.gateway.transport.ws.CLOSE_TIMEOUT", "5sec"),

    // Next property is to allow us to safely introduce changes
    // to conform with the WSE specification
    WSE_SPECIFICATION("com.kaazing.gateway.server.transport.wse.SPECIFICATION", "false"),

    // Next property is to allow us to safely introduce changes
    // to conform with the httpxe specification
    HTTPXE_SPECIFICATION("com.kaazing.gateway.server.transport.httpxe.SPECIFICATION", "false"),

    // We are deliberately changing the default that Netty uses (availableProcessors() * 2):
    TCP_PROCESSOR_COUNT("org.kaazing.gateway.server.transport.tcp.PROCESSOR_COUNT",
                        Integer.toString(getRuntime().availableProcessors())),

    // Thread Pool Size for background tasks
    BACKGROUND_TASK_THREADS
            ("org.kaazing.gateway.server.util.scheduler.BACKGROUND_TASK_THREADS",
                    Integer.toString(getRuntime().availableProcessors())),

    TCP_WRITE_TIMEOUT
            ("org.kaazing.gateway.server.transport.tcp.WRITE_TIMEOUT"),

    CONNECT_FOLLOW_REDIRECT_WITH_QUERY
            ("org.kaazing.gateway.transport.http.CONNECT_FOLLOW_REDIRECT_WITH_QUERY", "false"),

    // services
    BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES
            ("org.kaazing.gateway.server.service.broadcast.MAXIMUM_PENDING_BYTES"),

    // management
    MANAGEMENT_SESSION_THRESHOLD
            ("org.kaazing.gateway.management.SESSION_THRESHOLD", "500"),

    /**
     * Internal system property describing whether Agrona is enabled or not
     */
    AGRONA_ENABLED
            ("org.kaazing.gateway.management.AGRONA_ENABLED", "false"),

    /**
     * Gateway identifier property. This should be set for each gateway instance in order to
     * uniquely identify each gateway instance based on a business id.
     * The gateway identifiers should be different for each gateway.
     */
    GATEWAY_IDENTIFIER
            ("org.kaazing.gateway.server.GATEWAY_IDENTIFIER", ""),

    // Internal system property checking if a newer version of the Gateway is available
    UPDATE_CHECK("org.kaazing.gateway.server.UPDATE_CHECK", "true"),

    UPDATE_CHECK_SERVICE_URL("org.kaazing.gateway.server.UPDATE_CHECK_SERVICE_URL"),

    CLUSTER_BYPASS_AWS_CHECK("com.kaazing.gateway.cluster.bypass.aws.check", "false"),

    // TCP_IDLE_TIMEOUT will kill the session if nothing is written or read at nio level.
    // Note, the idle usage is using the mina netty idle timeout which may be set
    // by higher layers. Logic for this is in NioIdleFilter
    TCP_IDLE_TIMEOUT("org.kaazing.gateway.server.transport.tcp.IDLE_TIMEOUT", Integer.toString(0)),
    UDP_IDLE_TIMEOUT("org.kaazing.gateway.server.transport.udp.IDLE_TIMEOUT", "60");

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

    /**
     * Method returning an internal configuration boolean value
     * @param configuration
     * @return
     */
    public boolean getBooleanProperty(Properties configuration) {
        String value = getProperty(configuration);
        if (value == null) {
            return false;
        }
        return parseBoolean(value);
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
