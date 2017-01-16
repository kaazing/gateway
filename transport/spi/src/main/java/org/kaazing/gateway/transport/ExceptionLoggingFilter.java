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
package org.kaazing.gateway.transport;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

/**
 * This filter only logs exceptionCaught, sessionOpened and sessionClosed events.
 */
public class ExceptionLoggingFilter
    extends LoggingFilter {

    public ExceptionLoggingFilter(Logger logger, String format) {
        super(logger, format);
    }

    public ExceptionLoggingFilter(Logger logger) {
        super(logger, "%s");
    }

    public ExceptionLoggingFilter(Logger logger, IoSession session, String transportName) {
        super(logger, session, transportName);
    }

    @Override
    protected void logSessionCreated(IoSession session) {
        // Do not log
    }

    @Override
    protected void logMessageReceived(IoSession session, Object message) {
        // Do not log
    }

    @Override
    protected void logSessionIdle(IoSession session) {
        // Do not log
    }

    @Override
    protected void logFilterWrite(IoSession session, Object message) {
        // Do not log
    }
}
