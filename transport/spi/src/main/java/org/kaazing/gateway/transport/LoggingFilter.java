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

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LogLevel;
import org.slf4j.Logger;

import org.kaazing.gateway.transport.bridge.Message;

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

    public LoggingFilter(Logger logger) {
        this(logger, "%s");
    }

    public LoggingFilter(Logger logger, String format) {
        if (logger == null) {
            throw new NullPointerException("logger");
        }

        if (format == null) {
            throw new NullPointerException("format");
        }

        this.logger = logger;
        this.format = format;
        this.createdFormat = String.format("[%s] CREATED: {}", String.format(format, "{}"));
        this.openedFormat = String.format("[%s] OPENED: {}", String.format(format, "{}"));
        this.receivedFormat = String.format("[%s] RECEIVED: {}: {}", String.format(format, "{}", "{}"));
        this.sentFormat = String.format("[%s] SENT: {}: {}", String.format(format, "{}", "{}"));
        this.idleFormat = String.format("[%s] IDLE: {}", String.format(format, "{}"));
        this.exceptionFormat = String.format("[%s] EXCEPTION: {}: {}", String.format(format, "{}", "{}"));
        this.closedFormat = String.format("[%s] CLOSED: {}", String.format(format, "{}"));
        this.writeFormat = String.format("[%s] WRITE: {}: {}", String.format(format, "{}", "{}"));
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
    // the current log level is appropriate for these events with the default of
    // logging only on TRACE level
    protected boolean shouldLogSessionCreated() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogSessionOpened() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogMessageReceived() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogMessageSent() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogSessionIdle() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogExceptionCaught() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogSessionClosed() {
        return logger.isTraceEnabled();
    }

    protected boolean shouldLogFilterWrite() {
        return logger.isTraceEnabled();
    }

    protected void logSessionCreated(IoSession session) {
        if (shouldLogSessionCreated()) {
            log(logger, getLevel(), createdFormat, session.getId(), session);
        }
    }

    protected void logSessionOpened(IoSession session) {
        if (shouldLogSessionOpened()) {
            log(logger, getLevel(), openedFormat, session.getId(), session);
        }
    }

    protected void logMessageReceived(IoSession session, Object message) {
        if (shouldLogMessageReceived()) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            log(logger, getLevel(), receivedFormat, session.getId(), session, message);
        }
    }

    protected void logMessageSent(IoSession session, Object message) {
        if (shouldLogMessageSent()) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            log(logger, getLevel(), sentFormat, session.getId(), session, message);
        }
    }

    protected void logSessionIdle(IoSession session) {
        if (shouldLogSessionIdle()) {
            log(logger, getLevel(), idleFormat, session.getId(), session);
        }
    }

    protected void logExceptionCaught(IoSession session, Throwable cause) {
        if (shouldLogExceptionCaught()) {
            log(logger, getLevel(), exceptionFormat, session.getId(), session, cause);
        }
    }

    protected void logSessionClosed(IoSession session) {
        if (shouldLogSessionClosed()) {
            log(logger, getLevel(), closedFormat, session.getId(), session);
        }
    }

    protected void logFilterWrite(IoSession session, Object message) {
        if (shouldLogFilterWrite()) {
            if (message instanceof Message) {
                message = ((Message) message).toVerboseString();
            }

            log(logger, getLevel(), writeFormat, session.getId(), session, message);
        }
    }

    public static void log(Logger logger, LogLevel eventLevel, String message, Throwable cause) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, cause); return;
            case DEBUG : logger.debug(message, cause); return;
            case INFO  : logger.info(message, cause); return;
            case WARN  : logger.warn(message, cause); return;
            case ERROR : logger.error(message, cause); return;
            default    : return;
        }
    }

    public static void log(Logger logger, LogLevel eventLevel, String message, Object param) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param); return;
            case DEBUG : logger.debug(message, param); return;
            case INFO  : logger.info(message, param); return;
            case WARN  : logger.warn(message, param); return;
            case ERROR : logger.error(message, param); return;
            default    : return;
        }
    }

    public static void log(Logger logger, LogLevel eventLevel, String message, Object param1, Object param2) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param1, param2); return;
            case DEBUG : logger.debug(message, param1, param2); return;
            case INFO  : logger.info(message, param1, param2); return;
            case WARN  : logger.warn(message, param1, param2); return;
            case ERROR : logger.error(message, param1, param2); return;
            default    : return;
        }
    }

    public static void log(Logger logger, LogLevel eventLevel, String message, Object param1, Object param2, Object param3) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param1, param2, param3); return;
            case DEBUG : logger.debug(message, param1, param2, param3); return;
            case INFO  : logger.info(message, param1, param2, param3); return;
            case WARN  : logger.warn(message, param1, param2, param3); return;
            case ERROR : logger.error(message, param1, param2, param3); return;
            default    : return;
        }
    }
}
