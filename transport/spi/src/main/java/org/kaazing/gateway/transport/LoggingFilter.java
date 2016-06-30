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

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LogLevel;
import org.slf4j.Logger;

import static org.kaazing.gateway.resource.address.ResourceAddress.IDENTITY_RESOLVER;

import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;
import org.kaazing.gateway.util.Utils;

public class LoggingFilter extends IoFilterAdapter {

    private static final String HOST_PORT_FORMAT = "%s:%d";
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

    private enum Strategy {
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
                // Include stack trace except for IOException, where we only log stack trace of the cause,
                // if there is one, to avoid overkill in this common case which can be caused by abrupt
                // client connection close or inactivity timeout.
                Throwable stack = t instanceof IOException ? t.getCause() : t;
                if (stack != null) {
                    Utils.log(logger, level, message, stack);
                } else {
                    Utils.log(logger, level, message);
                }
            }
        }

        void log(Logger logger, String message, Object param1) {
            if (shouldLog(logger)) {
                Utils.log(logger, level, message, param1);
            }
        }

        void log(Logger logger, String message, Object param1, Object param2) {
            if (shouldLog(logger)) {
                Utils.log(logger, level, message, param1, param2);
            }
        }
    }

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
        this.receivedFormat = String.format("[%s] RECEIVED: {}", String.format(format, "{}", "{}"));
        this.sentFormat = String.format("[%s] SENT: {}", String.format(format, "{}", "{}"));
        this.idleFormat = String.format("[%s] IDLE", String.format(format, "{}"));
        this.exceptionFormat = String.format("[%s] EXCEPTION: %s", String.format(format, "%s"), "%s");
        this.closedFormat = String.format("[%s] CLOSED: {}", String.format(format, "{}"));
        this.writeFormat = String.format("[%s] WRITE: {}", String.format(format, "{}", "{}"));
    }

    public static boolean addIfNeeded(Logger logger, IoSession session, String transportName) {
        if (!logger.isInfoEnabled()) {
            return false;
        }
        String user = getUserIdentifier(session);
        String loggingFilterName = transportName + "#logging";
        String format = transportName + "#%s";
        if (user != null) {
            // Escape % in user in case it contains a scoped ipv6 address like "fe80:0:0:0:90ea:3ee4:77ad:77ec%15:61641"
            // so we have a valid format string
            format = format + " " + user.replace("%", "%%");
        }
        if (logger.isTraceEnabled()) {
            session.getFilterChain().addLast(loggingFilterName, new ObjectLoggingFilter(logger, format));
            return true;
        } else {
            session.getFilterChain().addLast(loggingFilterName, new ExceptionLoggingFilter(logger, format));
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
        // Ultimately we should treat unexpected exceptions as ERROR level
        // but we are cautious for now since we don't want customers to suddenly
        // see lots of exception stacks in the logs
        return Strategy.INFO;
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

    /**
     * Get a suitable identification for the user. For now this just consists of the TCP endpoint.
     * the HTTP-layer auth principal, etc.
     * @param session
     * @return
     */
    static String getUserIdentifier(IoSession session) {
        IoService service = session.getService();
        boolean isAcceptor = service instanceof IoAcceptor || service instanceof BridgeAcceptor;
        SocketAddress hostPortAddress = isAcceptor ? session.getRemoteAddress() : session.getLocalAddress();
        SocketAddress identityAddress = isAcceptor ? session.getLocalAddress() : session.getRemoteAddress();
        String identity = resolveIdentity(identityAddress, (IoSessionEx)session);
        String hostPort = getHostPort(hostPortAddress);
        return identity == null ? hostPort : format("%s %s", identity, hostPort);
    }

    /**
     * Method performing identity resolution - attempts to extract a subject from the current
     * IoSessionEx session
     * @param address
     * @param session
     * @return
     */
    private static String resolveIdentity(SocketAddress address, IoSessionEx session) {
        if (address instanceof ResourceAddress) {
            Subject subject = session.getSubject();
            if (subject == null) {
                subject = new Subject();
            }
            return resolveIdentity((ResourceAddress) address, subject);
        }
        return null;
    }

    /**
     * Method attempting to perform identity resolution based on the provided subject parameter and transport
     * It is attempted to perform the resolution from the highest to the lowest layer, recursively.
     * @param address
     * @param subject
     * @return
     */
    private static String resolveIdentity(ResourceAddress address, Subject subject) {
        IdentityResolver resolver = address.getOption(IDENTITY_RESOLVER);
        ResourceAddress transport = address.getTransport();
        if (resolver != null) {
            return resolver.resolve(subject);
        }
        if (transport != null) {
            return resolveIdentity(transport, subject);
        }
        return null;
    }

    /**
     * Method attempting to retrieve host port identifier
     * @param address
     * @return
     */
    private static String getHostPort(SocketAddress address) {
        if (address instanceof ResourceAddress) {
            ResourceAddress lowest = getLowestTransportLayer((ResourceAddress)address);
            return format(HOST_PORT_FORMAT, lowest.getResource().getHost(), lowest.getResource().getPort());
        }
        if (address instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress)address;
            // we don't use inet.toString() because it would include a leading /, for example "/127.0.0.1:21345"
            // use getHostString() to avoid a reverse DNS lookup
            return format(HOST_PORT_FORMAT, inet.getHostString(), inet.getPort());
        }
        return null;
    }

    /**
     * Method returning lowest transport layer
     * @param transport
     * @return
     */
    private static ResourceAddress getLowestTransportLayer(ResourceAddress transport) {
        if (transport.getTransport() != null) {
            return getLowestTransportLayer(transport.getTransport());
        }
        return transport;
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
