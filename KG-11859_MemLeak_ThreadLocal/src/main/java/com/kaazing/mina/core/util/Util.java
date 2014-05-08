/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.util;

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
