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
package org.kaazing.mina.netty.config;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.util.Properties;

public enum InternalSystemProperty {

    // Time after which we will quit the processTaskQueue method to allow the run loop to select again
    // (avoids starving pending reads or writes). Value zero means do not do this.
    MAXIMUM_PROCESS_TASKS_TIME("org.kaazing.netty.MAXIMUM_PROCESS_TASKS_TIME", "0"), // disabled by default

    // select() timeout used after quitting processTaskQueue due to it taking longer than MAXIMUM_PROCESS_TASKS_TIME
    // (if set to non-zero). Unit is MILLISECONDS.
    // The value used should be large enough to guarantee we do get socket readable/writable notification from
    // the kernel (selectNow, done if the value is 0, does not always seem to achieve this) but small enough
    // not to waste too much time if there are no ready ops.
    QUICK_SELECT_TIMEOUT("org.kaazing.netty.QUICK_SELECT_TIMEOUT", "0"), // use selectNow by default

    // A worker is serving multiple UDP child channels and they share an Agrona read queue.
    // Agrona uses the next power of 2 greater than or equal to the supplied value
    UDP_CHANNEL_READ_QUEUE_SIZE("org.kaazing.netty.UDP_CHANNEL_READ_QUEUE_SIZE", "16384");

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

    public Long getLongProperty(Properties configuration) {
        String value = getProperty(configuration);
        if (value == null) {
            return null;
        }
        return parseLong(value);
    }

    public String getPropertyName() {
        return name;
    }

    public boolean isSet(Properties configuration) {
        return configuration.containsKey(name);
    }

}
