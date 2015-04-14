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

package org.kaazing.gateway.transport;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;

import java.util.Properties;

public enum InternalSystemProperty {

    // Thread Pool Size for background tasks (see SchedulerProvider)
    BACKGROUND_TASK_THREADS("org.kaazing.gateway.server.util.scheduler.BACKGROUND_TASK_THREADS", Integer.toString(getRuntime().availableProcessors()));

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
