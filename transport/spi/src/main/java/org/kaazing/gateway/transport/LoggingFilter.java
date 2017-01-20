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

import static java.lang.Thread.currentThread;

import java.io.IOException;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LogLevel;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;

public class LoggingFilter extends IoFilterAdapter {

    private final Logger logger;
    private final String format;

    private final String createdFormat;
    private final String openedFormat;
    private final String receivedFormat;
    private final String sentFormat;
    private final String idleFormat;
    private final String exceptionFormat;
    private final String closedFormat;
    private final String writeFormat;

    protected boolean shouldForceMoveAfterCodec = true;

    protected enum Strategy {
        DEBUG(LogLevel.DEBUG),
        ERROR(LogLevel.ERROR),
        INFO(LogLevel.INFO),
        NONE(LogLevel.NONE),
        TRACE(LogLevel.TRACE),
        WARN(LogLevel.WARN);

        final LogLevel level;

        Strategy(LogLevel level) {
            this.level = level;
        }

        boolean shouldLog(Logger logger) {
            return LoggingFilter.shouldLog(logger, level);
        }

        void log(Logger logger, String message, Throwable t) {
            if (shouldLog(logger)) {
                // Include stack trace if log level is info, except for IOException, where we only log stack trace
                // of the cause, if there is one, and we only log if logger level is info or finer, to avoid overkill
                // in this common case which can be caused by abrupt client network connection close or inactivity timeout.
                boolean isIOException = t instanceof IOException;
                Throwable stack = isIOException ? t.getCause() : t;
                if (stack != null) {
                    LoggingFilter.log(logger, level, message, stack);
                } else {
                    LoggingFilter.log(logger, level, message);
                }
            }
        }

        void log(Logger logger, String message, Object param1) {
            if (shouldLog(logger)) {
                LoggingFilter.log(logger, level, message, param1);
            }
        }

        void log(Logger logger, String message, Object param1, Object param2) {
            if (shouldLog(logger)) {
                LoggingFilter.log(logger, level, message, param1, param2);
            }
        }
    }

    public LoggingFilter(Logger logger) {
        this(logger, "%s");
    }

    public LoggingFilter(Logger logger, IoSession session, String transportName) {
        this(logger, getLoggingFormat(session, transportName));
    }

    public LoggingFilter(Logger logger, String sessionIdFormat) {
        if (logger == null) {
            throw new NullPointerException("logger");
        }

        if (sessionIdFormat == null) {
            throw new NullPointerException("sessionIdFormat");
        }

        this.logger = logger;
        this.format = sessionIdFormat;
        this.createdFormat = String.format("[%s] CREATED: {}", String.format(sessionIdFormat, "{}"));
        this.openedFormat = String.format("[%s] OPENED: {}", String.format(sessionIdFormat, "{}"));
        this.receivedFormat = String.format("[%s] RECEIVED: {}", String.format(sessionIdFormat, "{}", "{}"));
        this.sentFormat = String.format("[%s] SENT: {}", String.format(sessionIdFormat, "{}", "{}"));
        this.idleFormat = String.format("[%s] IDLE", String.format(sessionIdFormat, "{}"));
        this.exceptionFormat = String.format("[%s] EXCEPTION: %s", String.format(sessionIdFormat, "%s"), "%s");
        this.closedFormat = String.format("[%s] CLOSED: {}", String.format(sessionIdFormat, "{}"));
        this.writeFormat = String.format("[%s] WRITE: {}", String.format(sessionIdFormat, "{}", "{}"));
    }

    public static boolean addIfNeeded(Logger logger, IoSession session, String transportName) {
        if (!logger.isInfoEnabled()) {
            return false;
        }
        String loggingFilterName = transportName + "#logging";
        if (logger.isTraceEnabled()) {
            session.getFilterChain().addLast(loggingFilterName, new ObjectLoggingFilter(logger, session, transportName));
            return true;
        } else {
            session.getFilterChain().addLast(loggingFilterName, new ExceptionLoggingFilter(logger, session, transportName));
            return true;
        }
    }

    public LogLevel getLevel() {
        LogLevel level = LogLevel.ERROR;
        if (logger.isTraceEnabled()) {
            level = LogLevel.TRACE;
        } else if (logger.isDebugEnabled()) {
            level = LogLevel.DEBUG;
        } else if (logger.isInfoEnabled()) {
            level = LogLevel.INFO;
        } else if (logger.isWarnEnabled()) {
            level = LogLevel.WARN;
        }
        return level;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public void sessionCreated(NextFilter nextFilter,
                               IoSession session)
        throws Exception {

        logSessionCreated(session);
        super.sessionCreated(nextFilter, session);

        // move to end-of-chain to log readable objects instead of buffers
        IoFilterChain filterChain = session.getFilterChain();
        Entry entry = filterChain.getEntry(this);

        // If we have encountered the max number of connections,
        // then entry might be null because it's already been removed.
        // Thus guard against NPEs here (KG-3646).
        if (entry != null) {
            entry.remove();
            filterChain.addLast(entry.getName(), entry.getFilter());
        }
    }

    @Override
    public void sessionOpened(NextFilter nextFilter,
                              IoSession session)
        throws Exception {

        logSessionOpened(session);
        super.sessionOpened(nextFilter, session);

        if (shouldForceMoveAfterCodec) {
            // Move after codec to log codec exceptions (should they occur) and decoded messages
            IoFilterChain filterChain = session.getFilterChain();
            Entry codecEntry = filterChain.getEntry(ProtocolCodecFilter.class);
            if (codecEntry != null) {
                Entry loggingEntry = filterChain.getEntry(this);
                assert (loggingEntry != null);
                loggingEntry.remove();
                codecEntry.addAfter(loggingEntry.getName(), loggingEntry.getFilter());
            }
        }
    }

    @Override
    public void messageReceived(NextFilter nextFilter,
                                IoSession session,
                                Object message)
        throws Exception {

        logMessageReceived(session, message);
        super.messageReceived(nextFilter, session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter,
                            IoSession session,
                            WriteRequest writeRequest)
        throws Exception {

        logMessageSent(session, writeRequest.getMessage());
        super.messageSent(nextFilter, session, writeRequest);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter,
                            IoSession session,
                            IdleStatus status)
        throws Exception {

        logSessionIdle(session);
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter,
                                IoSession session,
                                Throwable cause)
        throws Exception {

        logExceptionCaught(session, cause);
        super.exceptionCaught(nextFilter, session, cause);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter,
                              IoSession session)
        throws Exception {

        logSessionClosed(session);
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void filterWrite(NextFilter nextFilter,
                            IoSession session,
                            WriteRequest writeRequest)
        throws Exception {

        logFilterWrite(session, writeRequest.getMessage());
        super.filterWrite(nextFilter, session, writeRequest);
    }

    // log levels can change dynamically, let subclasses determine whether or not
    // the current log level is appropriate for these events with sensible
    // defaults here
    protected Strategy getSessionCreatedStrategy() {
        return Strategy.NONE;
    }

    protected Strategy getSessionOpenedStrategy() {
        return Strategy.INFO;
    }

    protected Strategy getMessageReceivedStrategy() {
        return Strategy.TRACE;
    }

    protected Strategy getMessageSentStrategy() {
        return Strategy.TRACE;
    }

    protected Strategy getSessionIdleStrategy() {
        return Strategy.TRACE;
    }

    protected Strategy getExceptionCaughtStrategy(Throwable exception) {
        // Up to 4.0.9 we were only logging exceptions at debug level.
        // Up to 5.3.0 we were logging exceptions at info level
        // We are now logging them at warn level, except for IOExceptions,
        // and including stack trace only when the logger level is info
        // (and when there's a cause in case of IOException)
        // Ultimately we should arguably log unexpected exceptions at error level
        // but we are cautious for now since we don't want customers to suddenly
        // see lots of exception stacks in the logs.
        return exception instanceof IOException ? Strategy.INFO : Strategy.WARN;
    }

    protected Strategy getSessionClosedStrategy() {
        return Strategy.INFO;
    }

    protected Strategy getFilterWriteStrategy() {
        return Strategy.TRACE;
    }

    protected void logSessionCreated(IoSession session) {
        getSessionCreatedStrategy().log(logger, createdFormat, session.getId(), session);
    }

    protected void logSessionOpened(IoSession session) {
        getSessionOpenedStrategy().log(logger, openedFormat, session.getId(), session);
    }

    protected void logMessageReceived(IoSession session, Object message) {
        if (getMessageReceivedStrategy().shouldLog(logger)) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            getMessageReceivedStrategy().log(logger, receivedFormat, session.getId(), message);
        }
    }

    protected void logMessageSent(IoSession session, Object message) {
        if (getMessageSentStrategy().shouldLog(logger)) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            getMessageSentStrategy().log(logger, sentFormat, session.getId(), message);
        }
    }

    protected void logSessionIdle(IoSession session) {
        getSessionIdleStrategy().log(logger, idleFormat, session.getId());
    }

    protected void logExceptionCaught(IoSession session, Throwable cause) {
        String causeMessage = cause.toString();
        if (cause.getCause() != null) {
            causeMessage += ", caused by " + cause.getCause().toString();
        }
        getExceptionCaughtStrategy(cause).log(logger, String.format(exceptionFormat, session.getId(), causeMessage), cause);
    }

    protected void logSessionClosed(IoSession session) {
        getSessionClosedStrategy().log(logger, closedFormat, session.getId(), session);
    }

    protected void logFilterWrite(IoSession session, Object message) {
        if (getFilterWriteStrategy().shouldLog(logger)) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            getFilterWriteStrategy().log(logger, writeFormat, session.getId(), message);
        }
    }

    protected static String getLoggingFormat(IoSession session, String transportName) {
        String user = LoggingUtils.getUserIdentifier(session);
        String format = transportName + "#%s";
        if (user != null) {
            // Escape % in user in case it contains a scoped ipv6 address like "fe80:0:0:0:90ea:3ee4:77ad:77ec%15:61641"
            // so we have a valid format string
            format = format + " " + user.replace("%", "%%");
        }
        return format;
    }

    private static boolean shouldLog(Logger logger, LogLevel level) {
        switch(level) {
        case DEBUG:
            return logger.isDebugEnabled();
        case ERROR:
            return logger.isErrorEnabled();
        case INFO:
            return logger.isInfoEnabled();
        case NONE:
            return false;
        case TRACE:
            return logger.isTraceEnabled();
        case WARN:
            return logger.isWarnEnabled();
        }
        return false;
    }

    private static void log(Logger logger, LogLevel eventLevel, String message) {
        switch (eventLevel) {
            case TRACE : logger.trace(message); return;
            case DEBUG : logger.debug(message); return;
            case INFO  : logger.info(message); return;
            case WARN  : logger.warn(message); return;
            case ERROR : logger.error(message); return;
            default    : return;
        }
    }

    private static void log(Logger logger, LogLevel eventLevel, String message, Throwable cause) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, cause); return;
            case DEBUG : logger.debug(message, cause); return;
            case INFO  : logger.info(message, cause); return;
            case WARN  : logger.warn(message, cause); return;
            case ERROR : logger.error(message, cause); return;
            default    : return;
        }
    }

    protected static void log(Logger logger, LogLevel eventLevel, String message, Object param) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param); return;
            case DEBUG : logger.debug(message, param); return;
            case INFO  : logger.info(message, param); return;
            case WARN  : logger.warn(message, param); return;
            case ERROR : logger.error(message, param); return;
            default    : return;
        }
    }

    private static void log(Logger logger, LogLevel eventLevel, String message, Object param1, Object param2) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param1, param2); return;
            case DEBUG : logger.debug(message, param1, param2); return;
            case INFO  : logger.info(message, param1, param2); return;
            case WARN  : logger.warn(message, param1, param2); return;
            case ERROR : logger.error(message, param1, param2); return;
            default    : return;
        }
    }

    public static void moveAfterCodec(IoSession session) {
        // move logging filter after codec to log readable objects instead of buffers
        // and make sure it catches and logs any protocol decoder or encoder exceptions
        IoFilterChain filterChain = session.getFilterChain();
        Entry loggingEntry = filterChain.getEntry(LoggingFilter.class);
        if (loggingEntry == null) {
            return; // fail fast when tracing not active
        }
        Entry codecEntry = filterChain.getEntry(ProtocolCodecFilter.class);
        if (codecEntry != null) {
            loggingEntry.remove();
            codecEntry.addAfter(loggingEntry.getName(), loggingEntry.getFilter());
        }
    }
}
