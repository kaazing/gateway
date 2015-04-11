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

package org.kaazing.gateway.service.broadcast;

// FIXME: Make InternalSystemProperty generic, maybe gateway.util?
//import static org.kaazing.gateway.server.config.InternalSystemProperty.BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT;
//import static org.kaazing.gateway.server.config.InternalSystemProperty.BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES;
import static org.kaazing.gateway.util.Utils.parseBoolean;
import static org.kaazing.gateway.util.Utils.parsePositiveInteger;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Gateway service of type "broadcast".
 */
public class BroadcastService implements Service {

    private final Logger logger = LoggerFactory.getLogger("service.broadcast");
    // FIXME: make this a logically named logger that does not look like a classname
    private final Logger gatewayLogger = LoggerFactory.getLogger("org.kaazing.gateway.server.Gateway");

    // FIXME: remove me
    // services
    private static final String BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES = "org.kaazing.gateway.server.service.broadcast.MAXIMUM_PENDING_BYTES";
    private static final String BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT = "org.kaazing.gateway.server.service.broadcast.DISCONNECT_CLIENTS_ON_RECONNECT"; // true or false
    // FIXME: end of remove me

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean reconnect;
    private BroadcastServiceHandler handler;
    private ServiceContext serviceContext;
    private Properties configuration;

    private URI acceptURI;
    private URI connectURI;
    private int reconnectDelay;

    private final ConnectTask connectTask;

    public BroadcastService() {
        this.reconnect = new AtomicBoolean(false);
        this.connectTask = new ConnectTask();
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("broadcast_reconnect", false);
    }

    @Override
    public String getType() {
        return "broadcast";
    }

    @Override
    public void init(final ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;

        boolean disconnectClientsOnReconnect = parseBoolean(
                BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT,
//                    BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT.getPropertyName(),
                configuration.getProperty(BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT),
//                    BROADCAST_SERVICE_DISCONNECT_CLIENTS_ON_RECONNECT.getProperty(configuration),
                false);
        long maximumScheduledWriteBytes = parsePositiveInteger(
                BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES,
//                BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES.getPropertyName(),
                configuration.getProperty(BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES),
//                BROADCAST_SERVICE_MAXIMUM_PENDING_BYTES.getProperty(configuration),
                Long.MAX_VALUE);
        if ( maximumScheduledWriteBytes != Long.MAX_VALUE ) {
            // The system property was specified
            gatewayLogger.info(String.format("Broadcast service: limiting maximum scheduled write bytes to %d",
                    maximumScheduledWriteBytes));
        }
        this.handler = new BroadcastServiceHandler(disconnectClientsOnReconnect, maximumScheduledWriteBytes,
                serviceContext.getLogger());

        Collection<URI> connectURIs = serviceContext.getConnects();
        ServiceProperties properties = serviceContext.getProperties();
        String accept = properties.get("accept");
        String reconnectDelay = properties.get("reconnect.delay");
        // TODO: change error message when connect property is documented
        if (accept == null && (connectURIs == null || connectURIs.isEmpty())) {
            throw new IllegalArgumentException("Missing required property: accept");
        }

        this.acceptURI = (accept != null) ? URI.create(accept) : null;
        this.connectURI = (connectURIs == null || connectURIs.isEmpty()) ? null : connectURIs.iterator().next();
        this.reconnectDelay = (reconnectDelay != null) ? Integer.parseInt(reconnectDelay) : 3000;
    }

    @Override
    public void start() throws Exception {
        reconnect.set(true);

        serviceContext.bind(serviceContext.getAccepts(), handler);
        serviceContext.bindConnectsIfNecessary(serviceContext.getConnects());

        try {
            if (acceptURI != null) {
                serviceContext.bind(Arrays.asList(acceptURI), handler.getListenHandler());
            }

            if (connectURI != null) {
                scheduler.schedule(connectTask, 0, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Unable to bind to resource: " + acceptURI, e);
        }
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        // defer until stop to allow connect to succeed and re-enable the service
        serviceContext.unbindConnectsIfNecessary(serviceContext.getConnects());
        
        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }

        connectTask.stop();
    }

    @Override
    public void quiesce() throws Exception {
        reconnect.set(true);

        if (serviceContext != null) {
            serviceContext.unbind(serviceContext.getAccepts(), handler);

            if (acceptURI != null) {
                serviceContext.unbind(Arrays.asList(acceptURI), handler.getListenHandler());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        scheduler.shutdownNow();
    }

    private final class ConnectTask implements Runnable {
        private final AtomicReference<IoSession> session;

        public ConnectTask() {
            session = new AtomicReference<>();
        }

        public void stop() {
            IoSession connection = session.get();
            if (connection != null && !connection.isClosing()) {
                connection.close(true);
            }
        }

        @Override
        public void run() {
            serviceContext.connect(connectURI, handler.getListenHandler(), null).addListener(new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    if (future.isConnected()) {
                        IoSession newSession = future.getSession();
                        newSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                            @Override
                            public void operationComplete(CloseFuture future) {
                                session.set(null);

                                if (reconnect.get()) {
                                    scheduler.schedule(connectTask, reconnectDelay, TimeUnit.MILLISECONDS);
                                }
                            }
                        });
                        session.set(newSession);
                    }
                    else {
                        scheduler.schedule(connectTask, reconnectDelay, TimeUnit.MILLISECONDS);
                    }
                }
            });
        }
    }
}
