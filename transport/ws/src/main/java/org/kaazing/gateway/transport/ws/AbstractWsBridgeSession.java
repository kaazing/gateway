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

package org.kaazing.gateway.transport.ws;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.http.bridge.filter.HttpLoginSecurityFilter;
import org.kaazing.gateway.transport.ws.extension.ActiveWsExtensions;
import org.kaazing.gateway.transport.ws.util.BridgeSessionIterator;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A shared capabilities superclass for all WebSocket bridge sessions.
 *
 * @param <S> AN IoSession type
 */
public abstract class AbstractWsBridgeSession<S extends IoSessionEx, B extends IoBufferEx> extends AbstractBridgeSession<S, B> {

    // This logger logs scheduled events, and must be mentioned explicitly to show up to customers.
    protected static final Logger logger = LoggerFactory.getLogger("session.scheduled");

    // This logger logs websocket logout events, and must be mentioned explicitly to show up to customers.
    protected static final Logger logoutLogger = LoggerFactory.getLogger("session.logout");

    protected BridgeServiceFactory bridgeServiceFactory;
    protected ResourceAddressFactory resourceAddressFactory;
    protected final WsSessionTimeoutCommand sessionTimeout;

    protected ScheduledExecutorService scheduler;
    protected DefaultLoginResult loginResult;
    protected final ActiveWsExtensions wsExtensions;

    public AbstractWsBridgeSession(int ioLayer, Thread ioThread, Executor ioExecutor, IoServiceEx service, IoProcessorEx<S> sIoProcessor, ResourceAddress localAddress,
                                   ResourceAddress remoteAddress, IoBufferAllocatorEx<B> allocator,
                                   Direction direction, DefaultLoginResult loginResult, ActiveWsExtensions wsExtensions) {
        super(ioLayer, ioThread, ioExecutor, service, sIoProcessor, localAddress, remoteAddress, allocator, direction);
        this.wsExtensions = wsExtensions == null ? ActiveWsExtensions.EMPTY : wsExtensions;
        this.loginResult = loginResult;
        this.sessionTimeout = new WsSessionTimeoutCommand(this);
    }

    public AbstractWsBridgeSession(IoServiceEx service, IoProcessorEx<S> sIoProcessor, ResourceAddress localAddress,
                                   ResourceAddress remoteAddress, IoSessionEx parent, IoBufferAllocatorEx<B> allocator,
                                   Direction direction, DefaultLoginResult loginResult, ActiveWsExtensions wsExtensions) {
        super(service, sIoProcessor, localAddress, remoteAddress, parent, allocator, direction);
        this.wsExtensions = wsExtensions == null ? ActiveWsExtensions.EMPTY : wsExtensions;
        this.loginResult = loginResult;
        this.sessionTimeout = new WsSessionTimeoutCommand(this);
    }

    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    /**
     * @param scheduler the scheduler to be used for scheduling commands.
     */
    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void setSubject(Subject subject) {
        super.setSubject(subject);
    }

    public void cancelSessionTimeoutCommand() {
        // Cancel the lifetime enforcement if we have closed already.
        cancelCommand(sessionTimeout);
    }

    /**
     * Start up all the scheduled commands for this session.
     *
     */
    public void startupScheduledCommands() {
        startupSessionTimeoutCommand();
    }

    /**
     * When this session closes, one should call this command
     * to shutdown the scheduled commands
     */
    public void shutdownScheduledCommands() {
        // Cancel the commands (releasing session references)
        cancelSessionTimeoutCommand();
    }

    public ActiveWsExtensions getWsExtensions() {
        return this.wsExtensions;
    }

    // KG-6256: Avoid recreating this command when an overlapping WSEB writer is being attached.
    private AtomicBoolean initSessionTimeoutCommand = new AtomicBoolean(false);

	/**
	 * Start up timer for the session timeout of the WebSocket session
	 */
    public void startupSessionTimeoutCommand() {
        if (initSessionTimeoutCommand.compareAndSet(false, true)) {
            final Long sessionTimeout = getSessionTimeout();
            if ( sessionTimeout != null && sessionTimeout > 0) {
                if ( logger.isTraceEnabled() ) {
                    logger.trace( "Establishing a session timeout of " + sessionTimeout + " seconds for WebSocket session (" + getId() + ").");
                }
                scheduleCommand(this.sessionTimeout, sessionTimeout);
            }
        }
    }

    private void scheduleCommand(WsScheduledCommand command, final long delay) {
        setAttribute(command.getScheduledFutureKey(), command.schedule(scheduler, delay, TimeUnit.SECONDS));
    }

    private void cancelCommand(final WsScheduledCommand command) {
        command.cancel((ScheduledFuture<?>) removeAttribute(command.getScheduledFutureKey()));
    }

   /**
     * The period of time (in seconds) to wait after creating this session before closing the session.
     * @return the period of time (in seconds) to wait after creating this session before closing the session.
     */
    public Long getSessionTimeout() {
        //  look in the login result...if it is success
        if ( loginResult != null && loginResult.getType() == LoginResult.Type.SUCCESS) {
            Long sessionTimeout = loginResult.getSessionTimeout();
            if ( sessionTimeout != null && sessionTimeout > 0 ) {
                return sessionTimeout;
            }
        }
        return null;
    }


    /**
     * Log out of the login context associated with this WebSocket session.
     * Used to clean up any login context state that should be cleaned up.
     */
    public void logout() {
        ResultAwareLoginContext loginContext = findLoginContext(this);
        if (loginContext != null) {
            try {
                loginContext.logout();
                if (logoutLogger.isDebugEnabled()) {
                    logoutLogger.debug("[ws/#" + getId() + "] Logout successful.");
                }
            } catch (LoginException e) {
                logoutLogger.trace("[ws/#" + getId() + "] Exception occurred logging out of this WebSocket session.", e);
            } finally {

                try {
                    BridgeSessionIterator iterator = new BridgeSessionIterator(this);
                    while ( iterator.hasNext() ) {
                        IoSession session = iterator.next();
                        HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY.remove(session);
                    }
                } catch (Exception e) {
                    if ( logoutLogger.isTraceEnabled() ) {
                        logoutLogger.trace("Exception during login context attribute removal", e);
                    }
                }
            }
        }
    }

    private ResultAwareLoginContext findLoginContext(AbstractWsBridgeSession<?, ?> wsSession) {
        ResultAwareLoginContext result;
        IoSession session = wsSession;
        do {
            result = HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY.get(session);
            if ( session instanceof  AbstractBridgeSession) {
                session = ((AbstractBridgeSession<?, ?>)session).getParent();
            } else {
                session = null;
            }
        } while (session != null && result == null);

        return result;
    }

}
