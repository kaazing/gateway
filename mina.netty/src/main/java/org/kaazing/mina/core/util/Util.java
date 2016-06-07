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
package org.kaazing.mina.core.util;

import static java.lang.String.format;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util extends IoFilterAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    public static void verifyInIoThread(IoSession session, Thread ioThread) {
        Thread current = Thread.currentThread();
        if (current != ioThread) {
            String error = format("expected current thread %s to match %s in session %s", current, ioThread, session);
            RuntimeException e = new RuntimeException(error);
            StackTraceElement[] stackTrace = e.getStackTrace();
            String caller = stackTrace[1].toString().replace(Util.class.getName(), Util.class.getSimpleName());
            error = String.format("%s: %s", caller , error);
            // TODO: remove this logging and use a LoggingChannelHandler
            //       to log the exception on the Netty pipeline instead?
            LOGGER.error(error, e);
            throw e;
        }
    }

}
