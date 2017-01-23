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
package org.kaazing.mina.util;

import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link ExceptionMonitor} implementation that logs uncaught
 * exceptions using {@link Logger}.
 * <p>
 * All {@link IoService}s have this implementation as a default exception
 * monitor.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultExceptionMonitor extends ExceptionMonitor {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultExceptionMonitor.class);

    @Override
    public void exceptionCaught(Throwable cause, IoSession s) {
        if (cause instanceof Error) {
            throw (Error) cause;
        }

        LOGGER.warn("Unexpected exception in session {}", s, cause);
    }
}
