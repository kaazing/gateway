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
import static org.kaazing.gateway.resource.address.uri.URIUtils.getQuery;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.URLUtils;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.sse.SseProtocol;
import org.kaazing.gateway.transport.ws.WsProtocol;
import org.kaazing.gateway.transport.wseb.WsebAcceptor;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.gateway.util.GL;


/**
 * Gateway service of type "balancer".
 */
// See http://confluence.kaazing.wan/display/DEV/Balancer+Architecture
public class HttpBalancerService implements Service {
    public static final String BALANCER_MAP_NAME = "balancerMap";
    public static final String MEMBERID_BALANCER_MAP_NAME = "memberIdBalancerMap";

    private WsebBalancerServiceHandler wsebHandler;
    private WsnBalancerServiceHandler wsnHandler;
    private ServiceContext serviceContext;
    private ClusterContext clusterContext;
    private TransportFactory transportFactory;

    public HttpBalancerService() {
    }

    @Override
    public String getType() {
        return "balancer";
    }

    @Resource(name = "clusterContext")
    public void setConnectionCapabilities(ClusterContext clusterContext) {
        this.clusterContext = clusterContext;
    }

    @Resource(name = "transportFactory")
    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
        wsebHandler = new WsebBalancerServiceHandler();
        wsebHandler.setAccepts(serviceContext.getAccepts());
        wsebHandler.setClusterContext(clusterContext);
        wsnHandler = new WsnBalancerServiceHandler();

        // Register the Gateway's connection capabilities with the handlers so that session counts are tracked
        wsebHandler.setTransportFactory(transportFactory);
        GL.info(GL.CLUSTER_LOGGER_NAME,"Finished  HttpBalancerService.init()");
        clusterContext.logClusterStateAtInfoLevel();
    }

    @Override
    public void start() throws Exception {
        final BridgeSessionInitializer<ConnectFuture> wsBalancerSessionInitializer = new BridgeSessionInitializerAdapter<ConnectFuture>() {

            private final BridgeSessionInitializer<ConnectFuture> preUpgradeHttpSessionInitializer = new BridgeSessionInitializerAdapter<ConnectFuture>() {
                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    HttpAcceptSession httpSession = (HttpAcceptSession) session;
                    List<String> availableBalanceeURIs = wsebHandler.getBalanceeURIs(httpSession.isSecure());
                    List<String> selectedBalanceeURIs;
                    if (availableBalanceeURIs.isEmpty()) {
                        selectedBalanceeURIs = Collections.emptyList();
                    } else {
                        String selectedBalanceeURI = availableBalanceeURIs.get((int) (Math.random() * availableBalanceeURIs.size()));
                        selectedBalanceeURIs = new ArrayList<>(1);
                        selectedBalanceeURIs.add(selectedBalanceeURI);
                        GL.debug(GL.CLUSTER_LOGGER_NAME, "HttpBalancerService initializeSession Selected Balancee URI: {}", selectedBalanceeURI);
                    }
                    IoSession parent = httpSession.getParent();
                    parent.setAttribute(HttpAcceptor.BALANCEES_KEY, selectedBalanceeURIs);
                }
            };

            @Override
            @SuppressWarnings("unchecked")
            public void initializeSession(IoSession session, ConnectFuture future) {
                WsnSession wsnSession = (WsnSession) session;
                if (wsnSession.isBalanceSupported()) {
                    IoSession parent = wsnSession.getParent();
                    List<String> selectedBalanceeURIs = (List<String>) parent.getAttribute(HttpAcceptor.BALANCEES_KEY);
                    wsnSession.setBalanceeURIs(selectedBalanceeURIs);
                }
            }

            @Override
            public BridgeSessionInitializer<ConnectFuture> getParentInitializer(Protocol protocol) {
                return (HttpProtocol.HTTP.equals(protocol) ? preUpgradeHttpSessionInitializer : null);
            }
        };

        serviceContext.bind(HttpBalancerService.toWsBalancerURIs(serviceContext.getAccepts(), serviceContext.getAcceptOptionsContext(), transportFactory), wsnHandler,
                wsBalancerSessionInitializer);
        serviceContext.bind(HttpBalancerService.toHttpBalancerURIs(serviceContext.getAccepts(), serviceContext.getAcceptOptionsContext(), transportFactory), wsebHandler);
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }
    }

    @Override
    public void quiesce() throws Exception {
        if (serviceContext != null) {
            serviceContext.unbind(HttpBalancerService.toHttpBalancerURIs(serviceContext.getAccepts(), serviceContext.getAcceptOptionsContext(), transportFactory), wsebHandler);
            serviceContext.unbind(HttpBalancerService.toWsBalancerURIs(serviceContext.getAccepts(), serviceContext.getAcceptOptionsContext(), transportFactory), wsnHandler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    /**
     * Converts a collection of WS URIs to their equivalent WSN balancer URIs.
     * 
     * @param uris
     *            the URIs to convert
     * @return the converted URIs
     * @throws Exception
     */
    private static Collection<String> toWsBalancerURIs(Collection<String> uris,
                                                    AcceptOptionsContext acceptOptionsCtx,
                                                    TransportFactory transportFactory) throws Exception {
        List<String> httpURIs = new ArrayList<>(uris.size());
        for (String uri : uris) {
            String schemeFromAcceptURI = uri.substring(0, uri.indexOf(':'));
            Protocol protocol = transportFactory.getProtocol(schemeFromAcceptURI);
            if( WsProtocol.WS.equals(protocol) || WsProtocol.WSS.equals(protocol)) {
                for (String scheme: Arrays.asList("wsn", "wsx")) {
                    boolean secure = protocol.isSecure();
                    String wsBalancerUriScheme = secure ? scheme+"+ssl" : scheme;
                    String httpAuthority = getAuthority(uri);
                    String httpPath = getPath(uri);
                    String httpQuery = getQuery(uri);
                    httpURIs.add(buildURIAsString(wsBalancerUriScheme, httpAuthority, httpPath, httpQuery, null));

                    // ensure that the accept-options is updated with bindings if they exist for ws and/or wss
                    // so that the Gateway binds to the correct host:port as per the service configuration.
                    String internalURI = acceptOptionsCtx.getInternalURI(uri);
                    if ((internalURI != null) && !internalURI.equals(uri)) {
                        String authority = getAuthority(internalURI);
                        acceptOptionsCtx.addBind(wsBalancerUriScheme, authority);
                    }
                }
            }
        }
        return httpURIs;
    }

    private static List<String> HTTP_TRANSPORTS = Arrays.asList("https", "http", "httpxe+ssl", "httpxe");

    /**
     * Converts a collection of WS URIs to their equivalent HTTP(S) balancer URIs.
     * 
     * @param uris
     *            the URIs to convert
     * @return the converted URIs
     * @throws Exception
     */
    private static Collection<String> toHttpBalancerURIs(Collection<String> uris,
                                                      AcceptOptionsContext acceptOptionsCtx,
                                                      TransportFactory transportFactory) throws Exception {
        List<String> httpURIs = new ArrayList<>(uris.size());
        for (String uri : uris) {
            Protocol protocol = transportFactory.getProtocol(getScheme(uri));
            boolean secure = protocol.isSecure();

            for ( int i = 0; i < HTTP_TRANSPORTS.size(); i+=2) {
                String httpScheme = secure ? HTTP_TRANSPORTS.get(i) : HTTP_TRANSPORTS.get(i+1); // "httpxe+ssl" : "httpxe";
                String httpAuthority = getAuthority(uri);
                String httpPath = getPath(uri);
                String httpQuery = getQuery(uri);
                if (WsProtocol.WS.equals(protocol) || WsProtocol.WSS.equals(protocol)) {
                    httpURIs.add(buildURIAsString(httpScheme, httpAuthority,
                    URLUtils.replaceMultipleSlashesWithSingleSlash(httpPath + WsebAcceptor.EMULATED_SUFFIX), httpQuery, null));

                    // ensure that the accept-options is updated with bindings if they exist for ws and/or wss
                    // so that the Gateway binds to the correct host:port as per the service configuration.
                    String internalURI = acceptOptionsCtx.getInternalURI(uri);
                    if ((internalURI != null) && !internalURI.equals(uri)) {
                        String authority = getAuthority(internalURI);
                        acceptOptionsCtx.addBind(httpScheme, authority);
                    }
                }
                else if (SseProtocol.SSE.equals(protocol) || SseProtocol.SSE_SSL.equals(protocol) ||
                         HttpProtocol.HTTP.equals(protocol) || HttpProtocol.HTTPS.equals(protocol)) {
                    httpURIs.add(buildURIAsString(httpScheme, httpAuthority, httpPath, httpQuery, null));
                }
                else {
                    // we do not balance anything other than http, ws and sse
                    throw new IllegalArgumentException("Invalid protocol in the balancer: " + uri);
                }
            }
        }
        return httpURIs;
    }
}
