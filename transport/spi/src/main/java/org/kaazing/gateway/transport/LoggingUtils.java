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
import static org.kaazing.gateway.resource.address.ResourceAddress.IDENTITY_RESOLVER;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.security.auth.Subject;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingUtils {
    private static final String HOST_PORT_FORMAT = "%s:%d";
    private static final TypedAttributeKey<String> LOG_ID_ATTRIBUTE
                = new TypedAttributeKey<>(LoggingUtils.class, "logId");

    private LoggingUtils() {
        // so as not to be instantiated
    }

    /**
     * Prepends short session details (result of getId) for the session in square brackets to the message.
     * @param message   the message to be logged
     * @param session   an instance of IoSessionEx
     * @return example: "[wsn#34 127.0.0.0.1:41234] this is the log message"
     */
    public static String addSession(String message, IoSession session) {
        return format("[%s] %s", getId(session), message);
    }

    /**
     * Gets the identifying attributes of a session as a short string useful for logging purposes.
     * Example return values: wsn#34 127.0.0.0.1:41234, wsn#34 joe 127.0.0.0.1:41234
     * @param session
     * @return a short string including the numerical session ID, transport name and user or host and port details
     */
    public static String getId(IoSession session) {
        String result = LOG_ID_ATTRIBUTE.get(session);
        if (result == null) {
            result = session.getTransportMetadata().getName() + "#" + session.getId();
            String user = LoggingUtils.getUserIdentifier(session);
            if (user != null) {
                result = result + " " + user;
            }
            LOG_ID_ATTRIBUTE.set(session, result);
        }
        return result;
    }

    /**
     * Logs an unexpected exception in the same way LoggingFilter would, using the transport
     * logger for the transport of the given session.
     */
    public static void log(IoSession session, Throwable t) {
        Logger logger = getTransportLogger(session);
        log(session, logger, t);
    }

    /**
     * Logs an unexpected exception in the same way LoggingFilter would.
     */
    public static void log(IoSession session, Logger logger, Throwable t) {
        log(session, logger, t.toString(), t);
    }

    /**
     * Logs an unexpected exception in the same way LoggingFilter would.
     * IOExceptions are treated specially because they are frequent and expected: they
     * are logged only if info level is enabled.
     */
    public static void log(IoSession session, Logger logger, String message, Throwable t) {
        boolean isIOException = t instanceof IOException;
        if (isIOException && !logger.isInfoEnabled()) {
            return;
        }
        if (logger.isWarnEnabled()) {
            String finalMessage = t.getCause() == null ? message : message + ", caused by " + t.getCause();
            finalMessage = addSession(finalMessage, session);
            if (isIOException) {
                logIOException(finalMessage, logger, t);
            }
            else if (logger.isInfoEnabled()) {
                logger.warn(finalMessage, t);
            }
            else {
                logger.warn(finalMessage);
            }
        }
    }

    /**
     * IOExceptions ("Network connectivity has been lost or transport was closed at other end")
     * can be frequent due to network timeouts (inactivity timeout) or loss of network connectivity
     * so we only print info level and include a stack trace of the cause, if there is one.
     */
    private static void logIOException(String message, Logger logger, Throwable t) {
        Throwable cause = t.getCause();
        if (cause != null) {
            logger.info(message, cause);
        } else {
            logger.info(message);
        }
    }

    /**
     * Get a suitable identification for the user. For now this just consists of the TCP endpoint.
     * the HTTP-layer auth principal, etc.
     * @param session
     * @return
     */
    static String getUserIdentifier(IoSession session) {
        boolean isAcceptor = isAcceptor(session);
        SocketAddress hostPortAddress = isAcceptor ? session.getRemoteAddress() : session.getLocalAddress();
        SocketAddress identityAddress = isAcceptor ? session.getLocalAddress() : session.getRemoteAddress();
        String identity = resolveIdentity(identityAddress, (IoSessionEx)session);
        String hostPort = getHostPort(hostPortAddress);
        return identity == null ? hostPort : format("%s %s", identity, hostPort);
    }

    private static boolean isAcceptor(IoSession session) {
        IoService service = session.getService();
        return service instanceof IoAcceptor || service instanceof BridgeAcceptor;
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
    static ResourceAddress getLowestTransportLayer(ResourceAddress transport) {
        if (transport.getTransport() != null) {
            return getLowestTransportLayer(transport.getTransport());
        }
        return transport;
    }

    private static Logger getTransportLogger(IoSession session) {
        String loggerName = "transport." + session.getService().getTransportMetadata().getName()
                + (isAcceptor(session) ? ".accept" : ".connect");
        return LoggerFactory.getLogger(loggerName);
    }

}
