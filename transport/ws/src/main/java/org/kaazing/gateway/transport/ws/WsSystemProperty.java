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
package org.kaazing.gateway.transport.ws;

import static java.lang.Integer.parseInt;

import java.util.Properties;

public enum WsSystemProperty {

    // TODO: eliminate all use of System.getProperty and pull all used properties into this class
    //       (e.g. in NioSocketAcceptor)

    // transports
    WS_ENABLED_TRANSPORTS("org.kaazing.gateway.transport.ws.ENABLED_TRANSPORTS"),
    // in org.kaazing.gateway.util.InternalSystemProperty:
    // WSE_IDLE_TIMEOUT("org.kaazing.gateway.server.transport.wse.IDLE_TIMEOUT", "60")
    WSE_IDLE_TIMEOUT("org.kaazing.gateway.transport.wse.IDLE_TIMEOUT", "60");

    private final String name;
    private final String defaultValue;

    WsSystemProperty(String propertyName) {
        this(propertyName, null);
    }

    WsSystemProperty(String name, String defaultValue) {
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
