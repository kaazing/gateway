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
package org.kaazing.gateway.transport.nio;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;

import java.util.Properties;

public enum NioSystemProperty {

    // TODO: eliminate all use of System.getProperty and pull all used properties into this class
    //       (e.g. in NioSocketAcceptor)

    // Keeping the c.k.g.server.transport for the property names for backward compatibility

    // transports
    DEBUG_NIOWORKER_POOL("NioWorkerPool.DEBUG"), // true or false
    // We are deliberately changing the default that Netty uses (availableProcessors() * 2 ):
    TCP_PROCESSOR_COUNT("org.kaazing.gateway.server.transport.tcp.PROCESSOR_COUNT", Integer.toString(getRuntime().availableProcessors())),

    // These Buffer sizes were used by Mina. I'm pretty sure they no longer apply.
    TCP_READ_BUFFER_SIZE("org.kaazing.gateway.server.transport.tcp.READ_BUFFER_SIZE"),
    TCP_MINIMUM_READ_BUFFER_SIZE("org.kaazing.gateway.server.transport.tcp.MINIMUM_READ_BUFFER_SIZE"),
    TCP_MAXIMUM_READ_BUFFER_SIZE("org.kaazing.gateway.server.transport.tcp.MAXIMUM_READ_BUFFER_SIZE"),
    TCP_WRITE_TIMEOUT("org.kaazing.gateway.server.transport.tcp.WRITE_TIMEOUT"),

    // Socket Channel Config options (SocketChannelIoSessionConfig)
    TCP_REUSE_ADDRESS("org.kaazing.gateway.server.transport.tcp.REUSE_ADDRESS", "true"),
    TCP_NO_DELAY("org.kaazing.gateway.server.transport.tcp.TCP_NO_DELAY"),
    TCP_BACKLOG("org.kaazing.gateway.server.transport.tcp.BACKLOG"),
    TCP_KEEP_ALIVE("org.kaazing.gateway.server.transport.tcp.KEEP_ALIVE"),
    TCP_RECEIVE_BUFFER_SIZE("org.kaazing.gateway.server.transport.tcp.RECEIVE_BUFFER_SIZE"),
    TCP_SEND_BUFFER_SIZE("org.kaazing.gateway.server.transport.tcp.SEND_BUFFER_SIZE"),
    TCP_SO_LINGER("org.kaazing.gateway.server.transport.tcp.SO_LINGER"),
    TCP_IP_TOS("org.kaazing.gateway.server.transport.tcp.IP_TOS"),
    
    // TCP_IDLE_TIMEOUT will kill the session if nothing is written or read at nio level.
    // Note, the idle usage is using the mina netty idle timeout which may be set
    // by higher layers. Logic for this is in NioIdleFilter
    TCP_IDLE_TIMEOUT("org.kaazing.gateway.server.transport.tcp.IDLE_TIMEOUT", Integer.toString(0)),
    UDP_IDLE_TIMEOUT("org.kaazing.gateway.server.transport.udp.IDLE_TIMEOUT", "60");

    private final String name;
    private final String defaultValue;

    NioSystemProperty(String propertyName) {
        this(propertyName, null);
    }

    NioSystemProperty(String name, String defaultValue) {
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
