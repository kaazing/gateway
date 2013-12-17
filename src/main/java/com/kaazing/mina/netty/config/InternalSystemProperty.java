/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.config;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.util.Properties;

public enum InternalSystemProperty {

    // Time after which we will quit the processTaskQueue method to allow the run loop to select again
    // (avoids starving pending reads or writes). Value zero means do not do this.
    MAXIMUM_PROCESS_TASKS_TIME("com.kaazing.netty.MAXIMUM_PROCESS_TASKS_TIME", "0"), // disabled by default

    // select() timeout used after quitting processTaskQueue due to it taking longer than MAXIMUM_PROCESS_TASKS_TIME
    // (if set to non-zero). Unit is MILLISECONDS.
    // The value used should be large enough to guarantee we do get socket readable/writable notification from
    // the kernel (selectNow, done if the value is 0, does not always seem to achieve this) but small enough
    // not to waste too much time if there are no ready ops.
    QUICK_SELECT_TIMEOUT("com.kaazing.netty.QUICK_SELECT_TIMEOUT", "0"),  // use selectNow by default

    // Number of times to retry read if it returns 0 when ready ops included OP_READ  (this is a JDK bug: read should not
    // return 0 in this case)
    READ_BACKOFF_TRIES("com.kaazing.netty.READ_BACKOFF_TRIES", "0"),  // off by default

    // Time to wait if read backoff is enabled. Unit is NANOSECONDS.
    READ_BACKOFF_DELAY("com.kaazing.netty.READ_BACKOFF_DELAY", "50000");  // 50 microseconds by default

    private final String name;
    private final String defaultValue;

    private InternalSystemProperty(String propertyName) {
        this(propertyName, null);
    }

    private InternalSystemProperty(String name, String defaultValue) {
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
