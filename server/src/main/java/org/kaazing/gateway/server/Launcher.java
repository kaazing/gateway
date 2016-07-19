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
package org.kaazing.gateway.server;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.util.GL;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    // Use the Gateway logger for startup messages.  Ideally this would be a named logger rather than a
    // classname to avoid any compatibility issues if things get refactored (and also to hide classnames
    // from the log4j-config.xml except for debugging/tracing information).
    private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);

    private GatewayContext context;

    private final GatewayObserver gatewayListener;

    public Launcher(GatewayObserver gatewayListener) {
        this.gatewayListener = gatewayListener;
    }

    public void init(GatewayContext context) throws Exception {
        gatewayListener.startingGateway(context);
        try {
            initInternal(context);
        } catch (Exception e) {
            // shut down gateway if there was an error during init
            destroy();
            throw e;
        }
    }

    private void initInternal(GatewayContext context) throws Exception {
        this.context = context;

        long startAt = System.currentTimeMillis();

        ClusterContext cluster = context.getCluster();
        if (cluster != null) {
            cluster.start();
        }

        Set<String> mappedURIs = new TreeSet<>();

        // Initialize all services (so we're in a known state), then start
        // all services.
        for (ServiceContext serviceContext : context.getServices()) {
            gatewayListener.initingService(serviceContext);
            serviceContext.init();
            gatewayListener.initedService(serviceContext);
        }

        for (ServiceContext serviceContext : context.getServices()) {
            gatewayListener.startingService(serviceContext);
            serviceContext.start();
            gatewayListener.startedService(serviceContext);
            AcceptOptionsContext ctx = serviceContext.getAcceptOptionsContext();
            Collection<String> serviceAccepts = serviceContext.getAccepts();
            for (String serviceAccept : serviceAccepts) {
                String mappedURI = ctx.getInternalURI(serviceAccept);
                if ((mappedURI == null) || mappedURI.equals(serviceAccept)) {
                    mappedURIs.add(serviceAccept);
                } else {
                    mappedURIs.add(serviceAccept + " @ " + URIUtils.getAuthority(mappedURI));
                }
            }
        }

        long startedAt = System.currentTimeMillis();

        // LOGGER.info("Starting server at " + String.format("%1$tF %1$tT", startAt));
        LOGGER.info("Starting server");
        if (!mappedURIs.isEmpty()) {
            LOGGER.info("Starting services");
            for (String mappedURI : mappedURIs) {
                LOGGER.info("  " + mappedURI);
            }
            LOGGER.info("Started services");
        }
        LOGGER.info("Started server successfully in " + String.format("%1$.3f secs", (startedAt - startAt) / 1000f)
                + " at " + String.format("%1$tF %1$tT", startAt));

        if (cluster != null) {
            // now that the Gateway has started, log what it knows about the cluster
            GL.debug(GL.CLUSTER_LOGGER_NAME, "Exit Gateway launcher initInternal");
            cluster.logClusterStateAtInfoLevel();
        }
    }

    public void destroy() throws Exception {
        long stopAt = System.currentTimeMillis();

        Set<String> boundURIs = new TreeSet<>();
        for (ServiceContext serviceContext : context.getServices()) {
            boundURIs.addAll(serviceContext.getAccepts());
            try {
                gatewayListener.stopingService(serviceContext);
                serviceContext.stop();
                gatewayListener.stoppedService(serviceContext);
                gatewayListener.destroyingService(serviceContext);
                serviceContext.destroy();
                gatewayListener.destroyedService(serviceContext);
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Failed to stop service", e);
                }
            }
        }

        gatewayListener.stoppedGateway(context);

        context.dispose();

        long stoppedAt = System.currentTimeMillis();

        LOGGER.info("Stopping server");
        if (!boundURIs.isEmpty()) {
            LOGGER.info("Stopping services");
            for (String boundURI : boundURIs) {
                LOGGER.info("  " + boundURI);
            }
            LOGGER.info("Stopped services");
        }
        LOGGER.info("Stopped server successfully in " + String.format("%1$.3f secs", (stoppedAt - stopAt) / 1000f)
                + " at " + String.format("%1$tF %1$tT", stopAt));
    }

    public static Logger getGatewayStartupLogger() {
        return LOGGER;
    }
}
