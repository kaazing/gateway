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

import static java.lang.String.format;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

public final class LoggingUtils {

    private LoggingUtils() {
        // so as not to be instantiated
    }

    public static void log(IoSession session, Logger logger, Throwable t) {
        log(session, logger, t.getMessage(), t);
    }

    public static void log(IoSession session, Logger logger, String message, Throwable t) {
        if (logger.isDebugEnabled()) {
            // don't print stack for IOExceptions ("Network connectivity has been lost or transport was closed at other end")
            Throwable stack = t instanceof IOException ? t.getCause() : t;
            if (stack != null) {
                logger.debug(includeSession(message, session), t);
            } else {
                logger.debug(includeSession(message, session));
            }
        } else if (logger.isInfoEnabled()) {
            logger.info(includeSession(message, session));
        }
    }

    public static String getId(IoSession session) {
        return session.getTransportMetadata().getName() + "#" + session.getId();
    }

    public static String includeSession(String message, IoSession session) {
        return format("[%s] %s", getId(session), message);
    }

}
