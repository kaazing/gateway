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
package org.kaazing.gateway.server.context.resolve;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.ResourceAddress.CONNECT_REQUIRES_INIT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.uri.URIUtils.buildURIAsString;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getAuthority;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getFragment;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPath;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getQuery;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;
import static org.kaazing.gateway.resource.address.uri.URIUtils.modifyURIScheme;
import static org.kaazing.gateway.resource.address.uri.URIUtils.resolve;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.server.service.AbstractSessionInitializer;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.TransportOptionNames;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.gateway.util.GL;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.IMap;

public class DefaultServiceContext implements ServiceContext {

    public static final String BALANCER_MAP_NAME = "balancerMap";
    public static final String MEMBERID_BALANCER_MAP_NAME = "memberIdBalancerMap";

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String[] EMPTY_REQUIRE_ROLES = new String[]{};

    private static final String AUTHENTICATION_CONNECT = "authenticationConnect";
    private static final String AUTHENTICATION_IDENTIFIER = "authenticationIdentifier";
    private static final String BALANCE_ORIGINS = "balanceOrigins";
    private static final String ENCRYPTION_KEY_ALIAS = "encryptionKeyAlias";
    private static final String GATEWAY_ORIGIN_SECURITY = "gatewayHttpOriginSecurity";
    private static final String ORIGIN_SECURITY = "originSecurity";
    private static final String REALMS = "realms";
    private static final String REQUIRED_ROLES = "requiredRoles";
    private static final String SERVICE_DOMAIN = "serviceDomain";
    private static final String TEMP_DIRECTORY = "tempDirectory";

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";

    private final String serviceType;
    private final String serviceName;
    private final String serviceDescription;
    private final Service service;
    private final File tempDir;
    private final File webDir;
    private final Collection<String> balances;
    private final Collection<String> accepts;
    private final Collection<String> connects;
    private final ServiceProperties properties;
    private final Map<String, String> mimeMappings;
    private final Map<String, ? extends Map<String, ? extends CrossSiteConstraintContext>> acceptConstraintsByURI;
    private final TransportFactory transportFactory;
    private List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI;
    private final String[] requireRoles;
    private final Map<String, ResourceAddress> bindings;
    private final ConcurrentMap<Long, IoSessionEx> activeSessions;
    private final Map<String, IoHandler> bindHandlers;
    private final ClusterContext clusterContext;
    private final AcceptOptionsContext acceptOptionsContext;
    private final ConnectOptionsContext connectOptionsContext;
    private final RealmContext serviceRealmContext;
    private final ResourceAddressFactory resourceAddressFactory;
    private final Key encryptionKey;
    private final Logger logger;
    private final SchedulerProvider schedulerProvider;
    private final boolean supportsAccepts;
    private final boolean supportsConnects;
    private final boolean supportsMimeMappings;
    private final int processorCount;
    private int hashCode = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<String, Object> serviceSpecificObjects;

    /**
     * Default Session Initializer
     */
    private IoSessionInitializer<ConnectFuture> sessionInitializer = new IoSessionInitializer<ConnectFuture>() {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            // FIXME:  Do we need serviceContext and resource address passed in to be effective?
            session.getFilterChain().addLast(SESSION_FILTER_NAME, new IoFilterAdapter<IoSessionEx>() {
                @Override
                protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
                    addActiveSession(session);
                    super.doSessionOpened(nextFilter, session);
                }

                @Override
                protected void doSessionClosed(NextFilter nextFilter, IoSessionEx session) throws Exception {
                    removeActiveSession(session);
                    super.doSessionClosed(nextFilter, session);
                }
            });
        }
    };

    private MonitoringEntityFactory monitoringFactory;

    public DefaultServiceContext(String serviceType, Service service) {
        this(serviceType,
                null,
                null,
                service,
                null,
                null,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                new DefaultServiceProperties(),
                EMPTY_REQUIRE_ROLES,
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                new DefaultAcceptOptionsContext(),
                new DefaultConnectOptionsContext(),
                null,
                null,
                null,
                true,
                true,
                false,
                1,
                TransportFactory.newTransportFactory(Collections.EMPTY_MAP),
                ResourceAddressFactory.newResourceAddressFactory()
        );
    }

    public DefaultServiceContext(String serviceType,
                                 String serviceName,
                                 String serviceDescription,
                                 Service service,
                                 File webDir,
                                 File tempDir,
                                 Collection<String> balances,
                                 Collection<String> accepts,
                                 Collection<String> connects,
                                 ServiceProperties properties,
                                 String[] requireRoles,
                                 Map<String, String> mimeMappings,
                                 Map<String, Map<String, CrossSiteConstraintContext>> crossSiteConstraints,
                                 ClusterContext clusterContext,
                                 AcceptOptionsContext acceptOptionsContext,
                                 ConnectOptionsContext connectOptionsContext,
                                 RealmContext serviceRealmContext,
                                 Key encryptionKey,
                                 SchedulerProvider schedulerProvider,
                                 boolean supportsAccepts,
                                 boolean supportsConnects,
                                 boolean supportsMimeMappings,
                                 int processorCount,
                                 TransportFactory transportFactory,
                                 ResourceAddressFactory resourceAddressFactory) {
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        this.service = service;
        this.webDir = webDir;
        this.tempDir = tempDir;
        this.balances = balances;
        this.accepts = accepts;
        this.connects = connects;
        this.properties = properties;
        this.requireRoles = requireRoles;
        this.mimeMappings = mimeMappings;
        this.acceptConstraintsByURI = crossSiteConstraints;
        this.bindings = new HashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.bindHandlers = new HashMap<>(4);
        this.clusterContext = clusterContext;
        this.acceptOptionsContext = acceptOptionsContext;
        this.serviceRealmContext = serviceRealmContext;
        this.connectOptionsContext = connectOptionsContext;
        this.encryptionKey = encryptionKey;
        this.logger = LoggerFactory.getLogger("service." + serviceType.replace("$", "_"));
        this.schedulerProvider = schedulerProvider;
        this.supportsAccepts = supportsAccepts;
        this.supportsConnects = supportsConnects;
        this.supportsMimeMappings = supportsMimeMappings;
        this.processorCount = processorCount;
        this.transportFactory = transportFactory;
        this.resourceAddressFactory = resourceAddressFactory;
        this.serviceSpecificObjects = new HashMap<>();
    }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof ServiceContext) {
            ServiceContext otherServiceContext = (ServiceContext) otherObject;
            if (this.serviceType.equals(otherServiceContext.getServiceType())) {
                Collection<String> otherAccepts = otherServiceContext.getAccepts();
                for (String uri : this.accepts) {
                    if (!otherAccepts.contains(uri)) {
                        return false;
                    }
                }
                // same type, same accepts, return true
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = Objects.hash(serviceType, accepts, getServiceName());
        }
        return hashCode;
    }

    @Override
    public int getProcessorCount() {
        return processorCount;
    }

    @Override
    public RealmContext getServiceRealm() {
        return serviceRealmContext;
    }

    @Override
    public String getAuthorizationMode() {
        if (serviceRealmContext != null &&
                serviceRealmContext.getAuthenticationContext() != null) {
            return serviceRealmContext.getAuthenticationContext().getAuthorizationMode();
        }
        return null;
    }

    @Override
    public String getSessionTimeout() {
        if (serviceRealmContext != null &&
                serviceRealmContext.getAuthenticationContext() != null) {
            return serviceRealmContext.getAuthenticationContext().getSessionTimeout();
        }
        return null;
    }

    @Override
    public String decrypt(String encrypted) throws Exception {
        ByteBuffer decoded = Encoding.BASE64.decode(ByteBuffer.wrap(encrypted.getBytes(UTF_8)));
        InputStream bin = IoBuffer.wrap(decoded).asInputStream();

        Cipher cipher = Cipher.getInstance(encryptionKey.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        DataInputStream in = new DataInputStream(new CipherInputStream(bin, cipher));
        try {
            return in.readUTF();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String encrypt(String plaintext) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Cipher cipher = Cipher.getInstance(encryptionKey.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        DataOutputStream out = new DataOutputStream(new CipherOutputStream(bos, cipher));
        out.writeUTF(plaintext);
        out.close();

        ByteBuffer encoded = Encoding.BASE64.encode(ByteBuffer.wrap(bos.toByteArray(), 0, bos.size()));
        return IoBuffer.wrap(encoded).getString(UTF_8.newDecoder());
    }

    @Override
    public AcceptOptionsContext getAcceptOptionsContext() {
        return acceptOptionsContext;
    }

    @Override
    public ConnectOptionsContext getConnectOptionsContext() {
        return connectOptionsContext;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getServiceDescription() {
        return serviceDescription;
    }

    @Override
    public Collection<String> getAccepts() {
        return accepts;
    }

    @Override
    public Collection<String> getBalances() {
        return balances;
    }

    @Override
    public Collection<String> getConnects() {
        return connects;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public ServiceProperties getProperties() {
        return properties;
    }

    @Override
    public String[] getRequireRoles() {
        return requireRoles;
    }

    @Override
    public Map<String, String> getMimeMappings() {
        return mimeMappings;
    }

    @Override
    public String getContentType(String fileExtension) {
        String contentType = fileExtension == null ? null : mimeMappings.get(fileExtension.toLowerCase());
        return contentType;
    }

    @Override
    public Map<String, ? extends Map<String, ? extends CrossSiteConstraintContext>> getCrossSiteConstraints() {
        return acceptConstraintsByURI;
    }

    @Override
    public File getTempDirectory() {
        return tempDir;
    }

    @Override
    public File getWebDirectory() {
        return webDir;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public SchedulerProvider getSchedulerProvider() {
        return schedulerProvider;
    }

    @Override
    public void bind(Collection<String> bindURIs, IoHandler handler) {
        bind(bindURIs, handler, acceptOptionsContext);
    }

    @Override
    public void bind(Collection<String> bindURIs, IoHandler handler, AcceptOptionsContext acceptOptionsContext) {
        bind(bindURIs, handler, acceptOptionsContext, null);
    }

    @Override
    public void bind(Collection<String> bindURIs, IoHandler handler, BridgeSessionInitializer<ConnectFuture>
            bridgeSessionInitializer) {
        bind(bindURIs, handler, acceptOptionsContext, bridgeSessionInitializer);
    }

    @Override
    public void bindConnectsIfNecessary(Collection<String> connectURIs) {

        for (String connectURI : connectURIs) {
            // TODO: services should bind ResourceAddress directly, rather than passing URIs here
            Map<String, Object> connectOptions = buildResourceAddressOptions(connectURI, connectOptionsContext);
            ResourceAddress connectAddress = resourceAddressFactory.newResourceAddress(connectURI, connectOptions);
            bindConnectIfNecessary(connectAddress);
        }

    }

    @Override
    public void unbindConnectsIfNecessary(Collection<String> connectURIs) {

        for (String connectURI : connectURIs) {
            // TODO: services should bind ResourceAddress directly, rather than passing URIs here
            Map<String, Object> connectOptions = buildResourceAddressOptions(connectURI, connectOptionsContext);
            ResourceAddress connectAddress = resourceAddressFactory.newResourceAddress(connectURI, connectOptions);
            unbindConnectIfNecessary(connectAddress);
        }

    }

    private void bindConnectIfNecessary(ResourceAddress connectAddress) {
        if (connectAddress.getOption(CONNECT_REQUIRES_INIT)) {
            final URI transportURI = connectAddress.getResource();
            final String transportSchemeName = transportURI.getScheme();
            Transport transport = transportFactory.getTransportForScheme(transportSchemeName);
            assert transport != null;
            transport.getConnector(connectAddress).connectInit(connectAddress);
        } else {
            ResourceAddress connectTransport = connectAddress.getOption(TRANSPORT);
            if (connectTransport != null) {
                bindConnectIfNecessary(connectTransport);
            }
        }
    }

    private void unbindConnectIfNecessary(ResourceAddress connectAddress) {
        if (connectAddress.getOption(CONNECT_REQUIRES_INIT)) {
            final URI transportURI = connectAddress.getResource();
            final String transportSchemeName = transportURI.getScheme();
            Transport transport = transportFactory.getTransportForScheme(transportSchemeName);
            assert transport != null;
            transport.getConnector(connectAddress).connectDestroy(connectAddress);
        } else {
            ResourceAddress connectTransport = connectAddress.getOption(TRANSPORT);
            if (connectTransport != null) {
                unbindConnectIfNecessary(connectTransport);
            }
        }
    }

    @Override
    public void bind(Collection<String> bindURIs,
                     IoHandler handler,
                     AcceptOptionsContext acceptOptionsContext,
                     final BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer) {
        if (handler == null) {
            throw new IllegalArgumentException("Cannot bind without handler");
        }

        for (String uri : bindURIs) {
            bindHandlers.put(uri, handler);
        }

        Map<Transport, List<String>> bindsByTransport = getURIsByTransport(bindURIs);

        // for each transport group, create resource address for URIs and bind to transport.
        for (Entry<Transport, List<String>> entry : bindsByTransport.entrySet()) {
            Transport transport = entry.getKey();
            List<String> transportAccepts = entry.getValue();

            for (String transportAccept : transportAccepts) {

                Map<String, Object> options = buildResourceAddressOptions(transportAccept, acceptOptionsContext);

                ResourceAddress address = resourceAddressFactory.newResourceAddress(transportAccept, options);

                bindInternal(address, handler, transport, sessionInitializer, bridgeSessionInitializer);
                bindings.put(transportAccept, address);
            }
        }

        //
        // After the service has been physically bound, update the cluster state to reflect this service as a possible
        // balance target.
        //
        if (balances != null && balances.size() > 0) {
            if (!accepts.containsAll(bindURIs)) {
                // if this bind() call is for URIs that aren't the service accept URIs (typically just the broadcast service's
                // accept property) then don't update balancer state.  Only service accept URIs are balance targets.
                return;
            }

            CollectionsFactory factory = clusterContext.getCollectionsFactory();
            if (factory != null) {
                Map<MemberId, Map<String, List<String>>> memberIdBalancerUriMap = factory.getMap(MEMBERID_BALANCER_MAP_NAME);
                if (memberIdBalancerUriMap == null) {
                    throw new IllegalStateException("MemberId to BalancerMap is null");
                }

                MemberId localMember = clusterContext.getLocalMember();
                Map<String, List<String>> memberBalanceUriMap = memberIdBalancerUriMap.get(localMember);
                if (memberBalanceUriMap == null) {
                    memberBalanceUriMap = new HashMap<>();
                }

                List<String> acceptUris = new ArrayList<>();
                if (accepts != null) {
                    acceptUris.addAll(accepts);
                }
                // Must use TreeSet when replace(x,y,z) or remove(x,y) is used instead of remove(x) ,
                // hazelcast map requires a ordered set to hash consistently.
                IMap<String, TreeSet<String>> sharedBalanceUriMap = factory.getMap(BALANCER_MAP_NAME);
                for (String balanceURI : balances) {
                    if (accepts != null) {
                        memberBalanceUriMap.put(balanceURI, acceptUris);

                        // get and add to the list here instead of overwriting it
                        TreeSet<String> balanceUris;
                        TreeSet<String> newBalanceUris;
                        do {
                            GL.debug(GL.CLUSTER_LOGGER_NAME, "In Bind: While loop for balanceURI: " + balanceURI);
                            balanceUris = sharedBalanceUriMap.get(balanceURI);
                            if (balanceUris == null) {
                                newBalanceUris = new TreeSet<>();
                                newBalanceUris.addAll(accepts);
                                balanceUris = sharedBalanceUriMap.putIfAbsent(balanceURI, newBalanceUris);
                                if (balanceUris == null) {
                                    GL.debug(GL.CLUSTER_LOGGER_NAME, "In bind: balancer uri == null");
                                    break;
                                }
                            }
                            newBalanceUris = new TreeSet<>(balanceUris);
                            newBalanceUris.addAll(accepts);
                            if (newBalanceUris.equals(balanceUris)) {
                                break;
                            }
                        } while (!sharedBalanceUriMap.replace(balanceURI, balanceUris, newBalanceUris));

                        GL.info(GL.CLUSTER_LOGGER_NAME, "Cluster member {}: service {} bound", localMember, serviceType);
                        GL.debug(GL.CLUSTER_LOGGER_NAME, "Added balance URIs {}, new global list is {}",
                                acceptUris, newBalanceUris);
                    }
                }

                memberIdBalancerUriMap.put(localMember, memberBalanceUriMap);
            }
        }
        GL.debug(GL.CLUSTER_LOGGER_NAME, "Exit Bind");
        clusterContext.logClusterState();
    }

    private Map<String, Object> buildResourceAddressOptions(String transportURI, AcceptOptionsContext acceptOptionsContext) {
        // options is a new HashMap
        final Map<String, Object> options = acceptOptionsContext.asOptionsMap();
        injectServiceOptions(transportURI, options);

        // TODO: Instead of null, perhaps ServiceContext provides a
        // set of next protocol possibilities that are bound individually.
        options.put(TransportOptionNames.NEXT_PROTOCOL, null);
        return options;
    }

    private Map<String, Object> buildResourceAddressOptions(String transportURI, ConnectOptionsContext connectOptionsContext) {
        // options is a new HashMap
        final Map<String, Object> options = connectOptionsContext.asOptionsMap();
        injectServiceOptions(transportURI, options);

        // TODO: Instead of null, perhaps ServiceContext provides a
        // set of next protocol possibilities that are bound individually.
        options.put(TransportOptionNames.NEXT_PROTOCOL, null);
        return options;
    }

    private void injectServiceOptions(String transportURI, Map<String, Object> options) {

        Map<String, ? extends CrossSiteConstraintContext> acceptConstraints = acceptConstraintsByURI.get(transportURI);
        if (acceptConstraints == null && "balancer".equals(serviceType)) {
            if (getPath(transportURI) != null && getPath(transportURI).endsWith("/;e")) {
                transportURI = resolve(transportURI, getPath(transportURI).
                                substring(0, getPath(transportURI).length() - "/;e".length()));
            }
            acceptConstraints = acceptConstraintsByURI.get(modifyURIScheme(transportURI, "ws"));
            if (acceptConstraints == null && transportFactory.getProtocol(getScheme(transportURI)).isSecure()) {
                acceptConstraints = acceptConstraintsByURI.get(modifyURIScheme(transportURI, "wss"));
            }
        }
        if (acceptConstraints != null) {
            // needed by cross origin bridge filter to cache resources
            options.put(format("http[http/1.1].%s", ORIGIN_SECURITY), acceptConstraints);
            options.put(format("http[x-kaazing-handshake].%s", ORIGIN_SECURITY), acceptConstraints);
            options.put(format("http[httpxe/1.1].%s", ORIGIN_SECURITY), acceptConstraints);
            options.put(format("http[httpxe/1.1].http[http/1.1].%s", ORIGIN_SECURITY), acceptConstraints);
        }
        // needed for silverlight
        options.put(format("http[http/1.1].%s", GATEWAY_ORIGIN_SECURITY), authorityToSetOfAcceptConstraintsByURI);
        options.put(format("http[x-kaazing-handshake].%s", GATEWAY_ORIGIN_SECURITY), authorityToSetOfAcceptConstraintsByURI);
        options.put(format("http[httpxe/1.1].%s", GATEWAY_ORIGIN_SECURITY), authorityToSetOfAcceptConstraintsByURI);
        options.put(format("http[httpxe/1.1].http[http/1.1].%s", GATEWAY_ORIGIN_SECURITY),
                authorityToSetOfAcceptConstraintsByURI);

        //needed for correct enforcement of same origin in clustered gateway scenarios (KG-9686)
        final Collection<String> balanceOriginUris = toHttpBalanceOriginURIs(getBalances());

        if (balanceOriginUris != null) {
            options.put(format("http[http/1.1].%s", BALANCE_ORIGINS), balanceOriginUris);
            options.put(format("http[x-kaazing-handshake].%s", BALANCE_ORIGINS), balanceOriginUris);
            options.put(format("http[httpxe/1.1].%s", BALANCE_ORIGINS), balanceOriginUris);
            options.put(format("http[httpxe/1.1].http[http/1.1].%s", BALANCE_ORIGINS), balanceOriginUris);
        }

        // needed by resources handler to serve (cached) resources
        // TODO: convert to HTTP cache concept and drop in as a filter instead?
        options.put(format("http[http/1.1].%s", TEMP_DIRECTORY), tempDir);

        // We only need this option for KG-3476: silently convert a directory
        // service configured with application- challenge scheme to a non-application scheme.
        boolean forceNativeChallengeScheme = "directory".equals(getServiceType());

        // Add realmName property and  based on whether the service
        // is protected, and whether it is application- or native- security that is desired.
        if (serviceRealmContext != null) {
            HttpRealmInfo[] realms = new HttpRealmInfo[] { newHttpRealm(serviceRealmContext) };

            final AuthenticationContext authenticationContext = serviceRealmContext.getAuthenticationContext();
            if (authenticationContext != null) {

                String challengeScheme = authenticationContext.getHttpChallengeScheme();
                boolean isApplicationChallengeScheme = challengeScheme.startsWith(AUTH_SCHEME_APPLICATION_PREFIX);

                if (isApplicationChallengeScheme && !forceNativeChallengeScheme) {
                    options.put(format("http[http/1.1].%s", REALMS),
                    		realms);
                    for (String optionPattern : asList("http[httpxe/1.1].%s", "http[x-kaazing-handshake].%s")) {
                        options.put(format(optionPattern, REQUIRED_ROLES),
                                getRequireRoles());
                        options.put(format(optionPattern, REALMS),
                        		realms);
                        // We need this to support reading legacy service properties during authentication.
                        // authentication-connect, authentication-identifier, encryption.key.alias, service.domain
                        // The negotiate properties are replaced with client-side capabilities to use different
                        // KDCs on a per-realm basis. The ksessionid cookie properties are needed to write the cookie
                        // out if needed (recycle authorization mode).
                        options.put(format(optionPattern, AUTHENTICATION_CONNECT),
                                getProperties().get("authentication.connect"));
                        options.put(format(optionPattern, AUTHENTICATION_IDENTIFIER),
                                getProperties().get("authentication.identifier"));
                        options.put(format(optionPattern, ENCRYPTION_KEY_ALIAS),
                                getProperties().get("encryption.key.alias"));
                        options.put(format(optionPattern, SERVICE_DOMAIN),
                                getProperties().get("service.domain"));
                    }
                }

                // TCP
                for (String optionPattern : asList("tcp.%s")) {
                    // NO REALM_NAME as this will be an accept/connect option
                    String tcpRealmOptionName = format(optionPattern, "realm");
                    String tcpRealmName = (String) options.get(tcpRealmOptionName);
                    if (tcpRealmName != null) {
                        // TODO, use REALMS TCP connect options
                        // check if it's the same as the configured realm
                        if (!serviceRealmContext.getName().equals(tcpRealmName)) {
                            logger.error("{} configuration error: {} needs to be set to the same value as the configured realm",
                                    serviceName, tcpRealmOptionName);
                            throw new IllegalArgumentException(
                                    tcpRealmOptionName + " needs to be the same as the configured realm");
                        }
                        options.put(format(optionPattern, "loginContextFactory"), serviceRealmContext.getLoginContextFactory());
                    }
                }

                // TODO: eliminate forceNativeChallengeScheme by locking down authentication schemes for "directory" service
                if (!isApplicationChallengeScheme || forceNativeChallengeScheme) {
                    String optionPattern = "http[http/1.1].%s";
                    options.put(format(optionPattern, REALMS),
                            realms);
                    options.put(format(optionPattern, REQUIRED_ROLES),
                            getRequireRoles());
                    // see note above for why this is needed
                    options.put(format(optionPattern, AUTHENTICATION_CONNECT),
                            getProperties().get("authentication.connect"));
                    options.put(format(optionPattern, AUTHENTICATION_IDENTIFIER),
                            getProperties().get("authentication.identifier"));
                    options.put(format(optionPattern, ENCRYPTION_KEY_ALIAS),
                            getProperties().get("encryption.key.alias"));
                    options.put(format(optionPattern, SERVICE_DOMAIN),
                            getProperties().get("service.domain"));

                }
            }
        }
    }

	private static HttpRealmInfo newHttpRealm(RealmContext serviceRealmContext) {
        final AuthenticationContext authenticationContext = serviceRealmContext.getAuthenticationContext();
        String name = serviceRealmContext.getName();
        String authorizationMode = authenticationContext.getAuthorizationMode();
        String challengeScheme = authenticationContext.getHttpChallengeScheme();
        String description = serviceRealmContext.getDescription();
        String[] headerNames = authenticationContext.getHttpHeaders();
        String[] parameterNames = authenticationContext.getHttpQueryParameters();
        String[] authenticationCookieNames = authenticationContext.getHttpCookieNames();
        LoginContextFactory loginContextFactory = serviceRealmContext.getLoginContextFactory();
        Collection<Class<? extends Principal>> userPrincipleClasses = loadUserPrincipalClasses(name, serviceRealmContext.getUserPrincipalClasses());

        return new DefaultHttpRealmInfo(name, challengeScheme, description, headerNames, parameterNames, authenticationCookieNames, loginContextFactory, userPrincipleClasses);
	}

    private static Collection<Class<? extends Principal>> loadUserPrincipalClasses(String name, String[] userPrincipalClasses) {
        Collection<Class<? extends Principal>> userPrincipals = new ArrayList<>();
        for (String className : userPrincipalClasses) {
            try {
                userPrincipals.add(Class.forName(className).asSubclass(Principal.class));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        format("%s%s%s", "Class ", className,
                                " could not be loaded. Please check the gateway configuration xml and confirm that"
                                        + " user-principal-class value(s) are spelled correctly for realm " + name + "."),
                        new ClassNotFoundException(e.getMessage()));
            }
        }
        return userPrincipals;
    }

    private Collection<String> toHttpBalanceOriginURIs(Collection<String> balances) {
        if (balances == null || balances.isEmpty()) {
            return balances;
        }

        List<String> result = new ArrayList<>(balances.size());
        for (String uri : balances) {
            if (uri != null) {
                try {
                    final String scheme = getScheme(uri);
                    if ("ws".equals(scheme)) {
                        result.add(buildURIAsString("http", getAuthority(uri),
                                  getPath(uri), getQuery(uri), getFragment(uri)));
                    } else if ("wss".equals(scheme)) {
                        result.add(buildURIAsString("https", getAuthority(uri),
                                  getPath(uri), getQuery(uri), getFragment(uri)));
                    } else {
                        result.add(uri);
                    }
                } catch (URISyntaxException e) {
                    if (logger.isDebugEnabled()) {
                        logger.warn(String.format("Cannot translate balanc uri '%s' into a http balance origin.", uri));
                    }
                }
            }
        }
        return result;
    }

    private void bindInternal(final ResourceAddress address,
                              final IoHandler handler,
                              final Transport transport,
                              final IoSessionInitializer<ConnectFuture> sessionInitializer,
                              final BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer) {
        BridgeAcceptor acceptor = transport.getAcceptor(address);
        try {
            acceptor.bind(address, handler, new BridgeSessionInitializer<ConnectFuture>() {
                @Override
                public BridgeSessionInitializer<ConnectFuture> getParentInitializer(Protocol protocol) {
                    return (bridgeSessionInitializer != null) ? bridgeSessionInitializer.getParentInitializer(protocol) : null;
                }

                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    sessionInitializer.initializeSession(session, future);

                    if (bridgeSessionInitializer != null) {
                        bridgeSessionInitializer.initializeSession(session, future);
                    }
                }
            });
        } catch (RuntimeException re) {
            // Catch this RuntimeException and add a bit more information
            // to its message (cf KG-1462)
            throw new RuntimeException(String.format("Error binding to %s: %s", address.getResource(), re.getMessage()), re);
        }
    }

    /**
     * Return the URIs organized by transport.
     * <p/>
     * NOTE: because this relies on gatewayContext, we cannot call it until after the service context has been completely set up
     * (and in fact not until GatewayContextResolver has constructed, as it doesn't create the DefaultGatewayContext until the
     * last step in construction.
     *
     * @param uris
     * @return
     */
    private Map<Transport, List<String>> getURIsByTransport(Collection<String> uris) {
        Map<Transport, List<String>> urisByTransport = new HashMap<>();

        // iterate over URIs and group them by transport
        for (String uri : uris) {
            String uriScheme = getScheme(uri);
            Transport transport = transportFactory.getTransportForScheme(uriScheme);
            List<String> list = urisByTransport.get(transport);
            if (list == null) {
                list = new ArrayList<>();
                urisByTransport.put(transport, list);
            }
            list.add(uri);
        }
        return urisByTransport;
    }

    @Override
    public void unbind(Collection<String> bindURIs, IoHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Cannot unbind without handler");
        } else {
            for (String uri : bindURIs) {
                IoHandler bindHandler = bindHandlers.get(uri);
                if (bindHandler != null) {
                    if (!handler.equals(bindHandler)) {
                        throw new IllegalArgumentException("Cannot unbind with a handler " + handler
                                + " different from the one used for binding " + bindHandler + " to URI " + uri);
                    }
                    bindHandlers.remove(uri);
                }
            }
        }

        //
        // If there are balance URIs on the service, update the cluster state before physically unbinding
        // so that this cluster member is removed from the balance targets first.  This avoids a race
        // condition where a new connection comes in *after* this cluster member has been unbound but
        // before the cluster state has been updated, resulting in this cluster member being incorrectly
        // picked as the balancee.
        //
        if (balances != null && balances.size() > 0) {
            CollectionsFactory factory = clusterContext.getCollectionsFactory();
            if (factory != null) {
                Map<MemberId, Map<String, List<String>>> memberIdBalancerUriMap = factory
                        .getMap(MEMBERID_BALANCER_MAP_NAME);
                if (memberIdBalancerUriMap == null) {
                    throw new IllegalStateException("MemberId to BalancerMap is null");
                }

                MemberId localMember = clusterContext.getLocalMember();

                Map<String, List<String>> memberBalanceUriMap = memberIdBalancerUriMap.get(localMember);
                if (memberBalanceUriMap == null) {
                    IllegalStateException is = new IllegalStateException(
                            "In unbind: Member balancerMap returned null for member " + localMember);
                    GL.error(GL.CLUSTER_LOGGER_NAME, "In unbind: Member balancerMap returned null for member "
                            + localMember + is);
                    throw is;
                }
                // Must use TreeSet when replace(x,y,z) or remove(x,y) is used instead of remove(x) , hazelcast map
                // requires a ordered set to hash consistently.
                IMap<String, TreeSet<String>> sharedBalanceUriMap = factory.getMap(BALANCER_MAP_NAME);
                for (String balanceURI : balances) {
                    if (accepts != null) {
                        memberBalanceUriMap.remove(balanceURI);

                        // get and add to the list here instead of overwriting it
                        TreeSet<String> balanceUris;
                        TreeSet<String> newBalanceUris = null;
                        do {
                            GL.debug(GL.CLUSTER_LOGGER_NAME,
                                    "In unbind while loop for balanaceURI: " + balanceURI);
                            boolean didRemove = false;
                            balanceUris = sharedBalanceUriMap.get(balanceURI);
                            if (balanceUris != null) {
                                GL.debug(GL.CLUSTER_LOGGER_NAME, "In unbind: balanceUris.size() :" + balanceUris.size());
                                newBalanceUris = new TreeSet<>(balanceUris);
                                for (String acceptUri : accepts) {
                                    didRemove = didRemove || newBalanceUris.remove(acceptUri);
                                }
                            }

                            if (!didRemove) {
                                // the current balancer entries were already removed, so since no work
                                // was done just skip the attempt to update cluster memory
                                break;
                            }
                            if (newBalanceUris.isEmpty()) {
                                GL.debug(GL.CLUSTER_LOGGER_NAME, "In unbind: newBalanceUris.isEmpty()");
                                if (sharedBalanceUriMap.remove(balanceURI, balanceUris)) {
                                    GL.debug(GL.CLUSTER_LOGGER_NAME, "In unbind: remove returned true now break");
                                    break;
                                } else {
                                    GL.debug(GL.CLUSTER_LOGGER_NAME, "In unbind: remove returned false now continue");
                                    continue; // start over to refresh the newBalanceUris
                                }
                            }
                        } while (!sharedBalanceUriMap.replace(balanceURI, balanceUris, newBalanceUris));

                        GL.info(GL.CLUSTER_LOGGER_NAME, "Cluster member {}: service {} unbound", localMember, serviceType);
                        GL.debug(GL.CLUSTER_LOGGER_NAME, "Removed balance URIs {}, new global list is {}", accepts,
                                newBalanceUris);
                    }
                }
                memberIdBalancerUriMap.put(localMember, memberBalanceUriMap);
            }
            GL.debug(GL.CLUSTER_LOGGER_NAME, "Exit Unbind");
            clusterContext.logClusterState();
        }

        for (String uri : bindURIs) {
            String uriScheme = getScheme(uri);
            Transport transport = transportFactory.getTransportForScheme(uriScheme);
            ResourceAddress address = bindings.remove(uri);
            if (address != null) {
                transport.getAcceptor(address).unbind(address);
            }
        }
    }

    @Override
    public ConnectFuture connect(String connectURI, final IoHandler connectHandler,
                                 final IoSessionInitializer<ConnectFuture> connectSessionInitializer) {
        ResourceAddress address = resourceAddressFactory.newResourceAddress(connectURI, connectOptionsContext.asOptionsMap());
        return connect(address, connectHandler, connectSessionInitializer);
    }

    @Override
    public ConnectFuture connect(ResourceAddress address, final IoHandler connectHandler,
                                 final IoSessionInitializer<ConnectFuture> connectSessionInitializer) {
        String uriScheme = getScheme(address.getExternalURI());
        Transport transport = transportFactory.getTransportForScheme(uriScheme);

        BridgeConnector connector = transport.getConnector(address);
        return connector.connect(address, connectHandler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                sessionInitializer.initializeSession(session, future);

                if (connectSessionInitializer != null) {
                    connectSessionInitializer.initializeSession(session, future);
                }
            }
        });
    }

    @Override
    public Collection<IoSessionEx> getActiveSessions() {
        return activeSessions.values();
    }

    @Override
    public IoSessionEx getActiveSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return activeSessions.get(sessionId);
    }

    @Override
    public void addActiveSession(IoSessionEx session) {
        activeSessions.put(session.getId(), session);
    }

    @Override
    public void removeActiveSession(IoSessionEx session) {
        activeSessions.remove(session.getId());
    }

    /**
     * Session initializer for the 'standard' case.
     */
    public final class StandardSessionInitializer extends AbstractSessionInitializer {

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            super.initializeSession(session, future);
            session.getFilterChain().addLast(SESSION_FILTER_NAME, new ServiceSessionFilter());
        }
    }

    public class ServiceSessionFilter extends IoFilterAdapter<IoSessionEx> {

        @Override
        protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
            activeSessions.put(session.getId(), session);
            super.doSessionOpened(nextFilter, session);
        }

        @Override
        protected void doSessionClosed(NextFilter nextFilter, IoSessionEx session) throws Exception {
            activeSessions.remove(session.getId());
            super.doSessionClosed(nextFilter, session);
        }
    }

    @Override
    public void init() throws Exception {
        getService().init(this);
    }

    @Override
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            getService().start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (started.compareAndSet(true, false)) {
            // So management won't get screwed up, don't allow the service
            // to add any more sessions than there already are.
            service.quiesce();
            service.stop();
        }
    }

    @Override
    public void destroy() throws Exception {
        getService().destroy();
    }

    @Override
    public boolean supportsAccepts() {
        return this.supportsAccepts;
    }

    @Override
    public boolean supportsConnects() {
        return this.supportsConnects;
    }

    @Override
    public boolean supportsMimeMappings() {
        return this.supportsMimeMappings;
    }

    @Override
    public void setListsOfAcceptConstraintsByURI(List<Map<String, Map<String, CrossSiteConstraintContext>>>
                                                             authorityToSetOfAcceptConstraintsByURI) {
        this.authorityToSetOfAcceptConstraintsByURI = authorityToSetOfAcceptConstraintsByURI;
    }

    @Override
    public Map<String, Object> getServiceSpecificObjects() {
        return serviceSpecificObjects;
    }

    @Override
    public IoSessionInitializer<ConnectFuture> getSessionInitializor() {
        return sessionInitializer;
    }

    @Override
    public void setSessionInitializor(IoSessionInitializer<ConnectFuture> ioSessionInitializer) {
        this.sessionInitializer = ioSessionInitializer;
    }

    @Override
    public MonitoringEntityFactory getMonitoringFactory() {
        return monitoringFactory;
    }

    @Override
    public void setMonitoringFactory(MonitoringEntityFactory monitoringFactory) {
        this.monitoringFactory = monitoringFactory;
    }

}
