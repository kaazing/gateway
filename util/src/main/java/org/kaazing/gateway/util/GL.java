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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GL {

    public static final String CLUSTER_LOGGER_NAME = "ha";

    protected GL() {
    }

    public static void info(String module, String format, Object ... args) {
        Logger logger = LoggerFactory.getLogger(module);
        if (logger.isInfoEnabled()) {
            logger.info(format, args);
        }
    }

    public static void debug(String module, String format, Object ... args) {
        Logger logger = LoggerFactory.getLogger(module);
        if (logger.isDebugEnabled()) {
            logger.debug(format, args);
        }
    }

    public static void trace(String module, String format, Object ... args) {
        Logger logger = LoggerFactory.getLogger(module);
        if (logger.isTraceEnabled()) {
            logger.trace(format, args);
        }
    }

    public static void error(String module, String format, Object ... args) {
        Logger logger = LoggerFactory.getLogger(module);
        if (logger.isErrorEnabled()) {
            logger.error(format, args);
        }
    }

    public static void warn(String module, String format, Object ... args) {
        Logger logger = LoggerFactory.getLogger(module);
        if (logger.isWarnEnabled()) {
            logger.warn(format, args);
        }
    }

    public static String identity(Object obj) {
        return obj.getClass().getSimpleName() + "@" + obj.hashCode();
    }
}
