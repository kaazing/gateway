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
package org.kaazing.gateway.service.http.balancer;

import static org.kaazing.gateway.resource.address.uri.URIUtils.buildURIAsString;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getAuthority;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPath;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.wseb.WsebAcceptor;
import org.kaazing.gateway.util.GL;


class WsebBalancerServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {
    private Collection<String> accepts;
    private ClusterContext clusterContext;
    private TransportFactory transportFactory;

    WsebBalancerServiceHandler() {
    }

    Collection<String> getAccepts() {
        return accepts;
    }

    void setAccepts(Collection<String> accepts) {
        this.accepts = new ArrayList<>(accepts);
    }

    ClusterContext getClusterContext() {
        return clusterContext;
    }

    void setClusterContext(ClusterContext clusterContext) {
        this.clusterContext = clusterContext;
    }

    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        // trigger sessionClosed to update connection capabilities accordingly
        session.close(true);
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        List<String> availableBalanceeURIs = getBalanceeURIs(session.isSecure());

        if (availableBalanceeURIs.isEmpty()) {
            GL.warn(GL.CLUSTER_LOGGER_NAME, "Rejected {} request for URI \"{}\" on session {}: no available balancee URI was found",                        session.getMethod(), session.getRequestURI(), session);
           session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
        } else {

            String selectedBalanceeURI = availableBalanceeURIs.get((int) (Math.random() * availableBalanceeURIs.size()));
            GL.debug(GL.CLUSTER_LOGGER_NAME, "WsebBalancerServiceHandler doSessionOpen Selected Balancee URI: {}", selectedBalanceeURI);

            URI requestURI = session.getRequestURI();
            String balanceeScheme = getScheme(selectedBalanceeURI);
            switch (balanceeScheme) {
                case "sse":
                    balanceeScheme = "http";
                    break;
                case "sse+ssl":
                    balanceeScheme = "https";
                    break;
                default:
                    balanceeScheme = getScheme(selectedBalanceeURI).replaceFirst("^ws", "http");
                    break;
            }
            String balanceePath = getPath(selectedBalanceeURI);
            String requestPath = requestURI.getPath();
            int emIndex = (requestPath != null) ? requestPath.indexOf(WsebAcceptor.EMULATED_SUFFIX) : -1;
            if ((emIndex != -1) && (!requestPath.contains(WsebAcceptor.EMULATED_SUFFIX + "/cookies"))) {
                balanceePath += requestPath.substring(emIndex);
            }
            String balanceeQuery = requestURI.getQuery();

            selectedBalanceeURI = buildURIAsString(balanceeScheme, getAuthority(selectedBalanceeURI), balanceePath, balanceeQuery, null);

            session.setStatus(HttpStatus.REDIRECT_FOUND /* 302 */);
            session.setWriteHeader("Location", selectedBalanceeURI);
        }
        session.close(false);
    }

    List<String> getBalanceeURIs(boolean secure) {
        List<String> balanceeURIs = new ArrayList<>();

        CollectionsFactory collectionsFactory = null;
        if (clusterContext != null) {
            collectionsFactory = clusterContext.getCollectionsFactory();
        }

        if (accepts != null &&
            collectionsFactory != null) {

            Lock mapLock = getLock(HttpBalancerService.BALANCER_MAP_NAME);
            try {
                mapLock.lock();
                // Get the map of balance URIs to accept URIs from the cluster.
                Map<String, Collection<String>> balancers = collectionsFactory.getMap(HttpBalancerService.BALANCER_MAP_NAME);

                // For my accept URIs, look up the map to get the balancee URIs for which I am balancing.
                for (String balancerAccept : accepts) {
                    Collection<String> balanceesForAccept = balancers.get(balancerAccept);
                    GL.debug("ha", String.format("Found balancee URIs %s for accept URI %s", balanceesForAccept, balancerAccept));

                    if (balanceesForAccept != null) {
                        for (String balanceeURI : balanceesForAccept) {
                            // Pick only clear or secure balancees as appropriate.
                            Protocol protocol = transportFactory.getProtocol(getScheme(balanceeURI));
                            if (secure == protocol.isSecure()) {
                                balanceeURIs.add(balanceeURI);
                            }
                        }
                    }
                }
            } finally {
                mapLock.unlock();
            }

        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Returning empty balancee URIs list: ");

            if (accepts == null) {
                sb.append("accepts are null, ");
            }

            if (clusterContext.getCollectionsFactory() == null) {
                sb.append("cluster context collections factory is null");
            }

            GL.debug("CLUSTER_LOGGER_NAME", sb.toString());
        }
        GL.debug(GL.CLUSTER_LOGGER_NAME,"Exit WsebBalancerService.getBalanceeURIs");
        clusterContext.logClusterState();
        return balanceeURIs;
    }


    public Lock getLock(String name) {
        return clusterContext.getLock(name);
    }

}
