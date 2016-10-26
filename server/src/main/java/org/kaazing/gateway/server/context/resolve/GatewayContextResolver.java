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

import static org.kaazing.gateway.resource.address.uri.URIUtils.buildURIAsString;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getAuthority;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getFragment;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getHost;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPath;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPort;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getQuery;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getUserInfo;
import static org.kaazing.gateway.service.util.ServiceUtils.LIST_SEPARATOR;
import static org.kaazing.gateway.util.feature.EarlyAccessFeatures.LOGIN_MODULE_EXPIRING_STATE;
import static org.kaazing.gateway.util.feature.EarlyAccessFeatures.TCP_REALM_EXTENSION;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.security.auth.BasicLoginModule;
import org.kaazing.gateway.security.auth.NegotiateLoginModule;
import org.kaazing.gateway.security.auth.TimeoutLoginModule;
import org.kaazing.gateway.server.ExpiringState;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.config.SchemeConfig;
import org.kaazing.gateway.server.config.june2016.AuthenticationType;
import org.kaazing.gateway.server.config.june2016.AuthorizationConstraintType;
import org.kaazing.gateway.server.config.june2016.ClusterConnectOptionsType;
import org.kaazing.gateway.server.config.june2016.ClusterType;
import org.kaazing.gateway.server.config.june2016.CrossSiteConstraintType;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.june2016.LoginModuleOptionsType;
import org.kaazing.gateway.server.config.june2016.LoginModuleType;
import org.kaazing.gateway.server.config.june2016.MimeMappingType;
import org.kaazing.gateway.server.config.june2016.RealmType;
import org.kaazing.gateway.server.config.june2016.SecurityType;
import org.kaazing.gateway.server.config.june2016.ServiceAcceptOptionsType;
import org.kaazing.gateway.server.config.june2016.ServiceConnectOptionsType;
import org.kaazing.gateway.server.config.june2016.ServiceDefaultsType;
import org.kaazing.gateway.server.config.june2016.ServicePropertiesType;
import org.kaazing.gateway.server.config.june2016.ServiceType;
import org.kaazing.gateway.server.config.parse.DefaultSchemeConfig;
import org.kaazing.gateway.server.context.DependencyContext;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.service.ServiceRegistry;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.cluster.ClusterConnectOptionsContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.util.GL;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.aws.AwsUtils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hazelcast.core.IMap;

public class GatewayContextResolver {

	public static final String AUTHORIZATION_MODE_CHALLENGE = "challenge";

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_BASIC = "Basic";
    public static final String AUTH_SCHEME_NEGOTIATE = "Negotiate";
    public static final String AUTH_SCHEME_APPLICATION_TOKEN = "Application Token";

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";

    enum HttpMethod {
        OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT
    }


    // This logger logs info and above to std out by default.
    private static final Logger LOGGER = Launcher.getGatewayStartupLogger();

    private static final String SERVICE_TYPE_CLASS_PREFIX = "class:";
    private static final String LOGIN_MODULE_TYPE_CLASS_PREFIX = "class:";

    private static final String EXPIRING_STATE_NAME = "ExpiringState";
    private static final String EXPIRING_STATE_OPTIONS_KEY = "ExpiringState";

    // a map of file-extension to mime-type.  For backward compatibility, we'll
    // hardcode this initial set based on the values in Dragonfire HttpUtils.getContentType().
    // TODO: In 4.0 we may want to remove this and require explicit settings!
    private static final Map<String, String> defaultMimeMappings = new HashMap<>();

    static {
        defaultMimeMappings.put("html", "text/html");
        defaultMimeMappings.put("htm", "text/html");
        defaultMimeMappings.put("js", "text/javascript");
        defaultMimeMappings.put("png", "image/png");
        defaultMimeMappings.put("gif", "image/gif");
        defaultMimeMappings.put("jpg", "image/jpeg");
        defaultMimeMappings.put("jpeg", "image/jpeg");
        defaultMimeMappings.put("css", "text/css");
        defaultMimeMappings.put("swf", "application/x-shockwave-flash");
        defaultMimeMappings.put("xap", "application/x-silverlight-app");
        defaultMimeMappings.put("htc", "text/x-component");
        defaultMimeMappings.put("jnlp", "application/x-java-jnlp-file");
        defaultMimeMappings.put("manifest", "text/cache-manifest");
        defaultMimeMappings.put("appcache", "text/cache-manifest");
        defaultMimeMappings.put("vtt", "text/vtt");
        defaultMimeMappings.put("aspx", "text/html");
    }

    private final Map<String, String> loginModuleClassNames;
    private final Map<String, LoginModuleControlFlag> loginModuleControlFlags;

    private final File webDir;
    private final File tempDir;
    private final MBeanServer jmxMBeanServer;
    private final Map<String, SchemeConfig> schemeConfigsByName;
    private final Map<String, DefaultTransportContext> transportContextsBySchemeName;
    private final Map<String, DefaultTransportContext> transportContextsByName;

    private ContextResolver<SecurityType, DefaultSecurityContext> securityResolver;

    private Map<String, Object> injectables = new HashMap<>();

    public GatewayContextResolver(File configDir, File webDir, File tempDir) {
        this(configDir, webDir, tempDir, null);
    }

    public GatewayContextResolver(File configDir, File webDir, File tempDir, MBeanServer mbeanServer) {
        this(new SecurityContextResolver(configDir, LOGGER), webDir, tempDir, mbeanServer);
    }

    public GatewayContextResolver(ContextResolver<SecurityType, DefaultSecurityContext> securityResolver,
                                  File webDir, File tempDir) {
        this(securityResolver, webDir, tempDir, null);
    }

    public GatewayContextResolver(ContextResolver<SecurityType, DefaultSecurityContext> securityResolver,
                                  File webDir, File tempDir, MBeanServer mbeanServer) {
        this.securityResolver = securityResolver;
        this.webDir = webDir;
        this.tempDir = tempDir;
        this.jmxMBeanServer = mbeanServer;
        this.loginModuleClassNames = new HashMap<>();

        Map<String, LoginModuleControlFlag> loginModuleControlFlags = new HashMap<>();
        loginModuleControlFlags.put("optional", LoginModuleControlFlag.OPTIONAL);
        loginModuleControlFlags.put("required", LoginModuleControlFlag.REQUIRED);
        loginModuleControlFlags.put("requisite", LoginModuleControlFlag.REQUISITE);
        loginModuleControlFlags.put("sufficient", LoginModuleControlFlag.SUFFICIENT);
        this.loginModuleControlFlags = loginModuleControlFlags;

        this.schemeConfigsByName = new HashMap<>();

        this.transportContextsBySchemeName = new HashMap<>();
        this.transportContextsByName = new HashMap<>();
    }

    public GatewayContext resolve(GatewayConfigDocument gatewayConfigDoc)
            throws Exception {
        return resolve(gatewayConfigDoc, System.getProperties());
    }

    public GatewayContext resolve(GatewayConfigDocument gatewayConfigDoc, Properties configuration) throws Exception {
        GatewayConfigDocument.GatewayConfig gatewayConfig = gatewayConfigDoc.getGatewayConfig();
        Collection<? extends SchemeConfig> schemeConfigs = new LinkedList<>();
        SecurityType[] securityConfigs = gatewayConfig.getSecurityArray();
        SecurityType securityConfig = (securityConfigs.length > 0) ? securityConfigs[securityConfigs.length - 1] : null;
        ServiceType[] serviceConfigs = gatewayConfig.getServiceArray();
        ServiceDefaultsType[] serviceDefaultsArray = gatewayConfig.getServiceDefaultsArray();
        ServiceDefaultsType serviceDefaults =
                (serviceDefaultsArray.length > 0) ? serviceDefaultsArray[serviceDefaultsArray.length - 1] : null;
        ClusterType[] clusterConfigs = gatewayConfig.getClusterArray();
        ClusterType clusterConfig = (clusterConfigs.length > 0) ? clusterConfigs[clusterConfigs.length - 1] : null;


        SchedulerProvider schedulerProvider = new SchedulerProvider(configuration);
        ClusterContext clusterContext = resolveCluster(clusterConfig, schedulerProvider);
        DefaultSecurityContext securityContext = securityResolver.resolve(securityConfig);
        ExpiringState expiringState = resolveExpiringState(clusterContext);
        RealmsContext realmsContext = resolveRealms(securityConfig, securityContext, configuration, clusterContext, expiringState);
        DefaultServiceDefaultsContext serviceDefaultsContext = resolveServiceDefaults(serviceDefaults);
        ServiceRegistry servicesByURI = new ServiceRegistry();
        Map<String, Object> dependencyContexts = resolveDependencyContext();
        ResourceAddressFactory resourceAddressFactory = resolveResourceAddressFactories();
        TransportFactory transportFactory = TransportFactory.newTransportFactory((Map) configuration);
        ServiceFactory serviceFactory = ServiceFactory.newServiceFactory();
        Collection<ServiceContext> services =
                resolveServices(servicesByURI, webDir, tempDir, serviceConfigs, securityContext,
                        realmsContext, clusterContext, serviceDefaults, schedulerProvider,
                        dependencyContexts,
                        configuration, transportFactory, serviceFactory, resourceAddressFactory, serviceDefaults);
        resolveTransports(transportFactory);

        BridgeServiceFactory bridgeServiceFactory = resolveBridgeServiceFactory(transportFactory);

        Map<String, DefaultSchemeContext> schemeContexts =
                resolveSchemes(services,
                        schemeConfigs,
                        configuration,
                        resourceAddressFactory);

        GatewayContext gatewayContext = new DefaultGatewayContext(schemeContexts,
                transportContextsBySchemeName,
                realmsContext,
                serviceDefaultsContext,
                services,
                servicesByURI,
                webDir,
                tempDir,
                clusterContext,
                schedulerProvider);

        injectables.putAll(dependencyContexts);
        injectables.put("serviceRegistry", servicesByURI);
        injectables.put("realmsContext", realmsContext);
        injectables.put("tempDirectory", tempDir);
        injectables.put("securityContext", securityContext);
        injectables.put("clusterContext", clusterContext);
        injectables.put("gatewayContext", gatewayContext);
        injectables.put("schedulerProvider", schedulerProvider);
        injectables.put("configuration", configuration);
        injectables.put("mbeanServer", jmxMBeanServer);
        injectables.put("bridgeServiceFactory", bridgeServiceFactory);
        injectables.put("resourceAddressFactory", resourceAddressFactory);
        injectables.put("transportFactory", transportFactory);
        injectables.put("expiringState", expiringState);
        gatewayContext.getInjectables().putAll(injectables);

        injectResources(services,
                    bridgeServiceFactory,
                    dependencyContexts,
                    injectables);

        return gatewayContext;
    }

    private BridgeServiceFactory resolveBridgeServiceFactory(TransportFactory transportFactory) {
        return new BridgeServiceFactory(transportFactory);
    }

    private ResourceAddressFactory resolveResourceAddressFactories() {
        return ResourceAddressFactory.newResourceAddressFactory();
    }

    private Map<String, DefaultSchemeContext> resolveSchemes(Collection<? extends ServiceContext> serviceContexts,
                                                             Collection<? extends SchemeConfig> schemeConfigs,
                                                             Properties configuration,
                                                             ResourceAddressFactory resourceAddressFactory)
            throws Exception {

        // load the default scheme information based on service accepts
        Set<String> schemeNames = new HashSet<>();
        for (ServiceContext serviceContext : serviceContexts) {
            for (String acceptURI : serviceContext.getAccepts()) {
                String schemeName = getScheme(acceptURI);
                schemeNames.add(schemeName);
            }
            for (String connectURI : serviceContext.getConnects()) {
                String schemeName = getScheme(connectURI);
                schemeNames.add(schemeName);
            }
            ServiceProperties properties = serviceContext.getProperties();
            String accept = properties.get("accept");
            if (accept != null) {
                accept = accept.trim();
                schemeNames.add(new URI(accept).getScheme());
            }
            String connect = properties.get("connect");
            if (connect != null) {
                connect = connect.trim();
                schemeNames.add(new URI(connect).getScheme());
            }
        }

        // Make sure that presence of ws(s) automatically implies presence of http(s) even if there are no accepts or connects
        // with that scheme.
        if (schemeNames.contains("ws")) {
            schemeNames.add("http");
            schemeNames.add("httpx");
            schemeNames.add("httpxe");
            schemeNames.add("wsn");
            schemeNames.add("wsx");
        }
        if (schemeNames.contains("wss")) {
            schemeNames.add("https");
            schemeNames.add("httpx+ssl");
            schemeNames.add("httpxe+ssl");
            schemeNames.add("wsn+ssl");
            schemeNames.add("wsx+ssl");
        }

        // SSE Schemes
        if (schemeNames.contains("sse")) {
            schemeNames.add("httpxe");
        }
        if (schemeNames.contains("sse+ssl")) {
            schemeNames.add("httpxe+ssl");
        }

        // Always add tcp
        schemeNames.add("tcp");

        // Always add udp
        schemeNames.add("udp");

        // override default scheme configuration
        for (SchemeConfig schemeConfig : schemeConfigs) {
            String schemeName = schemeConfig.getName();
            schemeConfigsByName.put(schemeName, schemeConfig);
            schemeNames.add(schemeName);
        }

        // resolve schemes
        Map<String, DefaultSchemeContext> schemeContexts = new HashMap<>();
        for (String schemeName : schemeNames) {
            DefaultSchemeContext schemeContext = schemeContexts.get(schemeName);
            if (schemeContext == null) {
                SchemeConfig schemeConfig = supplySchemeConfig(schemeName);

                int defaultPort = schemeConfig.getDefaultPort();
                String transportName = schemeConfig.getTransportName();
                DefaultTransportContext transportContext = transportContextsByName.get(transportName);
                if (transportContext == null) {
                    throw new IllegalArgumentException("Missing transport \"" + transportName + "\"");
                }

                schemeContext = new DefaultSchemeContext(schemeName, defaultPort, resourceAddressFactory);
                schemeContexts.put(schemeName, schemeContext);
                transportContextsBySchemeName.put(schemeName, transportContext);
            }
        }

        return schemeContexts;
    }

    private SchemeConfig supplySchemeConfig(String schemeName) {
        SchemeConfig schemeConfig = schemeConfigsByName.get(schemeName);
        if (schemeConfig == null) {
            schemeConfig = findSchemeConfig(schemeName);
            if (schemeConfig == null) {
                throw new IllegalArgumentException("Missing scheme \"" + schemeName + "\"");
            }
            if (schemeConfig != null) {
                schemeConfigsByName.put(schemeName, schemeConfig);
            }
        }
        return schemeConfig;
    }

    // Resolve service defaults into a config object so we can expose it as its
    // own object to management.
    private DefaultServiceDefaultsContext resolveServiceDefaults(ServiceDefaultsType serviceDefaults) {

        if (serviceDefaults == null) {
            return null;
        }

        DefaultAcceptOptionsContext acceptOptions = null;

        ServiceAcceptOptionsType serviceAcceptOptions = serviceDefaults.getAcceptOptions();
        if (serviceAcceptOptions != null) {
            acceptOptions = new DefaultAcceptOptionsContext(null, serviceAcceptOptions);
        }

        DefaultConnectOptionsContext connectOptions = null;
        ServiceConnectOptionsType serviceConnectOptions = serviceDefaults.getConnectOptions();
        if (serviceConnectOptions != null) {
            connectOptions = new DefaultConnectOptionsContext(null, serviceConnectOptions);
        }

        Map<String, String> mimeMappings = null;

        MimeMappingType[] mimeMappingTypes = serviceDefaults.getMimeMappingArray();
        if (mimeMappingTypes != null) {
            mimeMappings = new HashMap<>();
            for (MimeMappingType mmt : mimeMappingTypes) {
                mimeMappings.put(mmt.getExtension(), mmt.getMimeType());
            }
        }

        return new DefaultServiceDefaultsContext(acceptOptions, connectOptions, mimeMappings);
    }

    @SuppressWarnings("unchecked")
    private Collection<ServiceContext> resolveServices(ServiceRegistry serviceRegistry,
                                                       File webDir,
                                                       File tempDir,
                                                       ServiceType[] serviceConfigs,
                                                       SecurityContext securityContext,
                                                       RealmsContext realmsContext,
                                                       ClusterContext clusterContext,
                                                       ServiceDefaultsType defaultServiceConfig,
                                                       SchedulerProvider schedulerProvider,
                                                       Map<String, Object> dependencyContexts,
                                                       Properties configuration,
                                                       TransportFactory transportFactory,
                                                       ServiceFactory serviceFactory,
                                                       ResourceAddressFactory resourceAddressFactory,
                                                       ServiceDefaultsType serviceDefaults)
            throws Exception {

//        Map<String, Class<? extends Service>> serviceClasses = new HashMap<String, Class<? extends Service>>();
        Collection<ServiceContext> serviceContexts = new HashSet<>();

        // The list of mime mappings for a given service is a combination of the defaults we hardcoded,
        // any <mime-mapping> blocks from <service-defaults>, and any from <service>.
        Map<String, String> serviceDefaultsMimeMappings = new HashMap<>();
        serviceDefaultsMimeMappings.putAll(defaultMimeMappings);

        if (defaultServiceConfig != null) {
            // Note that we add the extensions as lower case for search consistency.
            for (MimeMappingType mimeMappingType : defaultServiceConfig.getMimeMappingArray()) {
                serviceDefaultsMimeMappings.put(mimeMappingType.getExtension().toLowerCase(), mimeMappingType.getMimeType());
            }
        }

        // Used by client access policy xml. This parameter is not fully initialized until after the service c
        List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI =
                new ArrayList<>();

        for (ServiceType serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.getName();
            String serviceDescription = serviceConfig.getDescription();
            String[] acceptStrings = serviceConfig.getAcceptArray();
            String[] balanceStrings = serviceConfig.getBalanceArray();
            String[] connectStrings = serviceConfig.getConnectArray();
            String serviceType = serviceConfig.getType();
            Service serviceInstance;

            Class<? extends Service> serviceClass;
            if (serviceType.startsWith(SERVICE_TYPE_CLASS_PREFIX)) {
                String className = serviceType.substring(SERVICE_TYPE_CLASS_PREFIX.length());
                try {
                    Class<?> clazz = Class.forName(className);
                    if (!Service.class.isAssignableFrom(clazz)) {
                        throw new IllegalArgumentException("Incompatible gateway service class: " + className);
                    }
                    serviceClass = (Class<? extends Service>) clazz;
                    serviceInstance = serviceClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown gateway service class: " + className);
                }
            } else {
                serviceInstance = serviceFactory.newService(serviceType);
                if (serviceInstance == null) {
                    throw new IllegalArgumentException("Unrecognized service type: " + serviceType);
                }
            }

            ServicePropertiesType propertiesType = serviceConfig.getProperties();
            DefaultServiceProperties properties = parsePropertiesType(propertiesType);

            // default ports
            Collection<String> acceptURIs = resolveURIs(acceptStrings);
            Collection<String> balanceURIs = resolveURIs(balanceStrings);
            Collection<String> connectURIs = resolveURIs(connectStrings);

            String acceptProperty = properties.get("accept");
            if (acceptProperty != null) {
                acceptProperty = acceptProperty.trim();
                acceptProperty = resolveURI(getCanonicalURI(acceptProperty, false));
                properties.put("accept", acceptProperty);
            }

            String connectProperty = properties.get("connect");
            if (connectProperty != null) {
                connectProperty = connectProperty.trim();
                properties.remove("connect");
                connectURIs.add(resolveURI(getCanonicalURI(connectProperty, true)));
            }

            Collection<String> requireRolesCollection = new LinkedList<>();
            for (AuthorizationConstraintType authConstraint : serviceConfig.getAuthorizationConstraintArray()) {
                Collections.addAll(requireRolesCollection, authConstraint.getRequireRoleArray());
            }
            RealmContext realmContext = null;
            String name = serviceConfig.getRealmName();
            if (serviceConfig.isSetRealmName()) {
                realmContext = realmsContext.getRealmContext(name);
                if (realmContext != null && !name.equals("auth-required")) {
                    if (requireRolesCollection.isEmpty()) {
                        Collections.addAll(requireRolesCollection, "*");
                    }
                }
            }
            String[] requireRoles = requireRolesCollection.toArray(new String[requireRolesCollection.size()]);

            // Add the service-specific mime mappings on top of the service-defaults+hardcoded.
            Map<String, String> mimeMappings = new HashMap<>();
            mimeMappings.putAll(serviceDefaultsMimeMappings);

            for (MimeMappingType mimeMappingType : serviceConfig.getMimeMappingArray()) {
                mimeMappings.put(mimeMappingType.getExtension().toLowerCase(), mimeMappingType.getMimeType());
            }

            Map<String, Map<String, CrossSiteConstraintContext>> acceptConstraintsByURI =
                    new HashMap<>();
            for (String acceptURI : acceptURIs) {
                int wildcardOriginCount = 0;
                CrossSiteConstraintType[] crossSiteConstraints = serviceConfig.getCrossSiteConstraintArray();
                for (CrossSiteConstraintType crossSiteConstraint : crossSiteConstraints) {
                    String allowOrigin = (String) crossSiteConstraint.getAllowOrigin();
                    String allowMethods = crossSiteConstraint.getAllowMethods();
                    String allowHeaders = crossSiteConstraint.getAllowHeaders();
                    BigInteger maximumAgeBigInt = crossSiteConstraint.getMaximumAge();
                    Integer maximumAge = (maximumAgeBigInt == null) ? null : maximumAgeBigInt.intValue();

                    if (allowOrigin == null) {
                        throw new IllegalArgumentException("Cross-site allow-origin is required");
                    }

                    if ("*".equals(allowOrigin)) {
                        wildcardOriginCount++;
                    } else {
                        String allowOriginURI = getCanonicalURI(allowOrigin, false);
                        allowOrigin = allowOriginURI;
                        String allowOriginScheme = getScheme(allowOriginURI);

                        if (!"http".equals(allowOriginScheme) && !"https".equals(allowOriginScheme)) {
                            throw new IllegalArgumentException(
                                    "Cross-site allow-origin must have URI syntax with http or https scheme");
                        }

                        if (getPath(allowOriginURI) != null && getQuery(allowOriginURI) != null
                                || getFragment(allowOriginURI) != null) {
                            throw new IllegalArgumentException(
                                    "Cross-site allow-origin must have URI syntax without path, query or fragment");
                        }

                        if (getPort(allowOriginURI) == -1) {
                            // default the port
                            if ("http".equals(allowOriginScheme)) {
                                allowOrigin += ":80";
                            } else if ("https".equals(allowOriginScheme)) {
                                allowOrigin += ":443";
                            } else {
                                throw new IllegalArgumentException("Unable to default port for scheme: \"" + allowOriginScheme
                                        + "\"");
                            }
                        }
                    }

                    if (allowMethods != null) {
                        String[] allowMethodsArray = allowMethods.split(",");
                        for (String allowMethod : allowMethodsArray) {
                            HttpMethod.valueOf(allowMethod);
                        }
                    } else {
                        // default the allow methods
                        allowMethods = "GET,POST";
                    }

                    Map<String, CrossSiteConstraintContext> acceptConstraints = acceptConstraintsByURI.get(acceptURI);
                    if (acceptConstraints == null) {
                        acceptConstraints = new HashMap<>();
                        acceptConstraintsByURI.put(acceptURI, acceptConstraints);
                    }

                    // add to authorityToSetOfAcceptConstraintsByURI
                    authorityToSetOfAcceptConstraintsByURI.add(acceptConstraintsByURI);

                    CrossSiteConstraintContext acceptConstraint = new DefaultCrossSiteConstraintContext(allowOrigin,
                            allowMethods, allowHeaders, maximumAge);
                    CrossSiteConstraintContext oldAcceptConstraint = acceptConstraints.put(allowOrigin, acceptConstraint);
                    if (oldAcceptConstraint != null) {
                        throw new IllegalArgumentException("Duplicate cross-site-constraint for service " + acceptURI
                                + " with allow-origin " + allowOrigin);
                    }
                }
                // verify that if there's a wildcard <cross-site-constraint> it is the only one
                if (wildcardOriginCount > 0 && crossSiteConstraints.length > 1) {
                    throw new IllegalArgumentException("Conflicting cross site constraints specified for service \"" + acceptURI
                            + "\". Remove the wildcard to specify more restrictive cross site constraints");
                }

                String host = getHost(acceptURI);
                if (host == null || host.isEmpty()) {
                    throw new IllegalArgumentException("Host is required for service \"" + acceptURI + "\".");
                }

                // verify that wild-card is singleton, if present
                if (requireRolesCollection.contains("*") && requireRolesCollection.size() > 1) {
                    throw new IllegalArgumentException("Conflicting security constraints specified for service \"" + acceptURI
                            + "\". Remove the wildcard to specify restricted roles");
                }
            }

            RealmContext serviceRealmContext = null;
            final String realmName = serviceConfig.getRealmName();
            if (serviceConfig.isSetRealmName()) {
                serviceRealmContext = realmsContext.getRealmContext(realmName);
                if (serviceRealmContext == null) {
                    throw new IllegalArgumentException("Unrecognized realm name \"" + realmName + "\".");
                }
            }

            ServiceAcceptOptionsType acceptOptions = serviceConfig.getAcceptOptions();
            ServiceAcceptOptionsType defaultOptionsConfig =
                    (defaultServiceConfig != null) ? defaultServiceConfig.getAcceptOptions() : null;
            AcceptOptionsContext acceptOptionsContext = new DefaultAcceptOptionsContext(acceptOptions, defaultOptionsConfig);
            if (acceptOptionsContext.asOptionsMap().containsKey("tcp.realm")) {
                TCP_REALM_EXTENSION.assertEnabled(configuration, LOGGER);
            }

            ServiceConnectOptionsType connectOptions = serviceConfig.getConnectOptions();

            ServiceConnectOptionsType defaultConnectOptions =
                    (serviceDefaults == null) ? ServiceConnectOptionsType.Factory.newInstance() : serviceDefaults
                            .getConnectOptions();
            ConnectOptionsContext connectOptionsContext =
                    new DefaultConnectOptionsContext(connectOptions, defaultConnectOptions);

            Key encryptionKey = null;

            if (serviceRealmContext == null &&
                    requireRolesCollection.size() > 0) {

                throw new IllegalArgumentException("Authorization constraints require a " +
                        "specified realm-name for service \"" + serviceDescription + "\"");
            }

            DefaultServiceContext serviceContext =
                    new DefaultServiceContext(serviceType, serviceName, serviceDescription, serviceInstance, webDir,
                            tempDir, balanceURIs, acceptURIs, connectURIs,
                            properties, requireRoles,
                            mimeMappings, acceptConstraintsByURI, clusterContext,
                            acceptOptionsContext, connectOptionsContext, serviceRealmContext,
                            encryptionKey, schedulerProvider,
                            supportsAccepts(serviceType),
                            supportsConnects(serviceType),
                            supportsMimeMappings(serviceType),
                            InternalSystemProperty.TCP_PROCESSOR_COUNT.getIntProperty(configuration),
                            transportFactory,
                            resourceAddressFactory);

            serviceContexts.add(serviceContext);

            // register service for each acceptURI
            for (String acceptURI : acceptURIs) {
                // verify we have a port set, otherwise set to default for scheme
                String authority = getAuthority(acceptURI);
                if (authority.indexOf(':') == -1) {
                    SchemeConfig schemeConfig = supplySchemeConfig(getScheme(acceptURI));
                    authority += ":" + schemeConfig.getDefaultPort();
                    acceptURI = getScheme(acceptURI) + "://" + authority + getPath(acceptURI);
                }
                serviceRegistry.register(acceptURI, serviceContext);
            }
        }

        for (ServiceContext ctxt : serviceContexts) {
            ctxt.setListsOfAcceptConstraintsByURI(authorityToSetOfAcceptConstraintsByURI);
        }
        return serviceContexts;
    }

    private DefaultServiceProperties parsePropertiesType(ServicePropertiesType propertiesType) {
        DefaultServiceProperties properties = new DefaultServiceProperties();
        if (propertiesType != null) {
            parseProperties(propertiesType.getDomNode(), properties);
        }
        return properties;
    }

    private void parseProperties(Node parent, ServiceProperties properties) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                NodeList content = node.getChildNodes();
                String nodeValue = "";
                boolean isSimpleProperty = true;
                for (int j = 0; j < content.getLength(); j++) {
                    Node child = content.item(j);
                    if (child != null) {
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            isSimpleProperty = false;
                            ServiceProperties newProperties = new DefaultServiceProperties();
                            properties.getNested(node.getLocalName(), true).add(newProperties);
                            parseProperties(node, newProperties);
                            break;
                        } else if (child.getNodeType() == Node.TEXT_NODE) {
                            // GatewayConfigParser skips white space so we don't need to trim. We concatenate in case
                            // the parser coughs up text content as more than one Text node.
                            String fragment = child.getNodeValue();
                            if (fragment != null) {
                                nodeValue = nodeValue + fragment;
                            }
                        }
                        // Skip over other node types
                    }
                }
                if (isSimpleProperty) {
                    // TODO; consider going to dynamically typed objects
                    // (i.e. allowing Object as properties value)
                    // For now if we see a property that is a list, we convert to comma separated list
                    String existingValue = properties.get(node.getLocalName());
                    if (existingValue == null) {
                        properties.put(node.getLocalName(), nodeValue);
                    } else {
                        properties.put(node.getLocalName(), existingValue + LIST_SEPARATOR + nodeValue);
                    }
                }
            }
        }
    }

    private Collection<String> resolveURIs(String[] acceptURIs) throws URISyntaxException {
        Collection<String> urisWithPort = new HashSet<>();
        for (String uri : acceptURIs) {
            String resolvedURI = resolveURI(getCanonicalURI(uri, true));
            urisWithPort.add(resolvedURI);
        }
        return urisWithPort;
    }

    private String resolveURI(String uri) throws URISyntaxException {
        String schemeName = getScheme(uri);
        SchemeConfig schemeConfig = supplySchemeConfig(schemeName);
        int defaultPort = schemeConfig.getDefaultPort();
        if (getPort(uri) == -1) {
            if (defaultPort == -1) {
                LOGGER.error("Missing port number in URI \"" + uri
                        + "\". You must include an explicit port number in this URI in your gateway configuration file.");
                throw new IllegalArgumentException("Missing port for URI \"" + uri + "\"");
            }
            if (defaultPort != 0) {
                String host = getHost(uri);
                String path = getPath(uri);
                String query = getQuery(uri);
                String fragment = getFragment(uri);
                uri = buildURIAsString(schemeName, null, host, defaultPort, path, query, fragment);
            }
        } else {
            if (defaultPort == 0) {
                LOGGER.error("Port number not allowed in URI \"" + uri
                        + "\". You must remove the port number from this URI in your gateway configuration file.");
                throw new IllegalArgumentException("Port not allowed in URI \"" + uri + "\"");
            }
        }
        return uri;
    }

    private ClusterContext resolveCluster(ClusterType clusterConfig,
                                          SchedulerProvider schedulerProvider) {
        if (clusterConfig == null) {
            return new StandaloneClusterContext();
        }

        String name = clusterConfig.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid name in the cluster configuration");
        }
        name = name.trim();

        final ClusterConnectOptionsType connectOptions = clusterConfig.getConnectOptions();
        ClusterConnectOptionsContext connectOptionsContext = new ClusterConnectOptionsContext() {
            @Override
            public String getAwsSecretKey() {
                return connectOptions == null ? null : connectOptions.getAwsSecretKey();
            }

            @Override
            public String getAwsAccessKeyId() {
                return connectOptions == null ? null : connectOptions.getAwsAccessKeyId();
            }
        };

        List<MemberId> accepts = processClusterMembers(clusterConfig.getAcceptArray(), "<accept>", null, -1);

        // The first accepts port is the port used by all network interfaces.
        int clusterPort = (accepts.size() > 0) ? accepts.get(0).getPort() : -1;

        List<MemberId> connects =
                processClusterMembers(clusterConfig.getConnectArray(), "<connect>", connectOptions, clusterPort);

        return new DefaultClusterContext(clusterConfig.getName(),
                accepts,
                connects,
                schedulerProvider,
                connectOptionsContext);
    }

    private List<MemberId> processClusterMembers(String[] collection,
                                                 String processing,
                                                 ClusterConnectOptionsType connectOptions,
                                                 int clusterPort) {
        List<MemberId> memberIds = new ArrayList<>();
        if (collection != null) {
            for (String member : collection) {
                String uri;
                try {
                    uri = getCanonicalURI(member, true);
                } catch (IllegalArgumentException ex) {
                    GL.error("ha", "Unrecognized {} url {} resulted in exception {}", processing, member, ex);
                    throw new IllegalArgumentException("Invalid URL in the cluster configuration:" + member, ex);
                }

                String scheme = getScheme(uri);
                if ((scheme.equals("tcp")) || scheme.equals("udp") || scheme.equals("aws")) {
                    int port = getPort(uri);
                    if (port == -1) {
                        GL.error("ha", "Port number is missing while processing {} for {}", processing, member);
                        throw new IllegalArgumentException("Invalid port number specified for " + processing + ": " + member);
                    }
                    String host = getHost(uri);
                    if (scheme.equals("aws")) {
                        // There should be ONLY one <connect></connect> tag with
                        // aws:// scheme in the <cluster></cluster> tag for
                        // AWS auto-discovery.
                        validateAwsClusterDiscovery(uri, connectOptions, processing, clusterPort, collection.length);
                    }

                    memberIds.add(new MemberId(scheme, host, port, getPath(uri)));
                } else {
                    GL.error("ha", "Unrecognized scheme {} for {} in {}", getScheme(uri), processing, member);
                    throw new IllegalArgumentException("Invalid scheme " + getScheme(uri) + " in the URL for " +
                    processing + " in " + member);
                }
            }
        }
        return memberIds;
    }

    private void validateAwsClusterDiscovery(String uri,
                                             ClusterConnectOptionsType connectOptions,
                                             String processing,
                                             int clusterPort,
                                             int collectionLength) {
        if (!AwsUtils.isDeployedToAWS() || !processing.equals("<connect>")) {
            GL.error("ha", "Unrecognized scheme {} for {} in {}",
                    getScheme(uri), processing, uri);
            throw new IllegalStateException("Invalid scheme " + getScheme(uri)
                    + " in the URL for " + processing + " in " + uri);
        }

        if (connectOptions == null) {
            GL.error("ha",
                    "Missing <connect-options> in the <cluster> when using auto-discovery");
            throw new IllegalStateException("Missing <connect-options> in <cluster> when using auto-discovery");
        }

        if (collectionLength > 1) {
            GL.error("ha",
                    "Only one {} element should  be specified in <cluster> for auto-discovery",
                    processing);
            throw new IllegalStateException("Only one <connect> tag should be specified in <cluster> for auto-discovery");
        }

        if (clusterPort != getPort(uri)) {
            // For the Peer Gateway we should ensure that the clusterPort from the
            // <accept></accept> tag matches the one specified in
            // the <connect>aws://security-group:<port>/groupName</connect> tag.
            GL.error("ha", "Mismatch in port numbers {} and {}", clusterPort, getPort(uri));
            throw new IllegalArgumentException("Port numbers on the network interface in <accept> and the member in <connect> " +
                    "do not match");
        }

        String scheme = getScheme(uri);
        if (!scheme.equalsIgnoreCase("aws")) {
            throw new IllegalStateException("Invalid scheme '" + scheme +
                    "' specified in the URI " + uri +
                    " instead of 'aws:'");
        }

        String host = getHost(uri);
        if (!host.equalsIgnoreCase("security-group")) {
            throw new IllegalStateException("Invalid host '" + host +
                    "' specified in the URI " + uri +
                    " instead of 'security-group'");
        }

        String accessKeyId = connectOptions.getAwsAccessKeyId();
        String secretKey = connectOptions.getAwsSecretKey();
        if (accessKeyId == null) {
            GL.error("ha", "Missing <aws.access-key-id> element in <connect-options>");
            throw new IllegalStateException("Missing <aws.access-key-id> element in the <connect-options>");
        }

        if (secretKey == null) {
            GL.error("ha", "Missing <aws.secret-key> element in <connect-options>");
            throw new IllegalStateException("Missing <aws.secret-key> element in the <connect-options>");
        }
    }

	private ExpiringState resolveExpiringState(ClusterContext clusterContext) {
        Supplier<IMap<Object, Object>> supplier = () -> clusterContext.getCollectionsFactory().getMap(EXPIRING_STATE_NAME);
		return new DefaultExpiringState(supplier);
	}

    private RealmsContext resolveRealms(SecurityType securityConfig, SecurityContext securityContext, Properties configuration,
        ClusterContext clusterContext, ExpiringState expiringState) {
        Map<String, DefaultRealmContext> realmContexts = new HashMap<>();

        if (securityConfig != null) {
            for (RealmType realmConfig : securityConfig.getRealmArray()) {
                String name = realmConfig.getName();

                // Check for multiple <realm> configurations using the
                // same name (KG-7237).
                if (realmContexts.get(name) != null) {
                    throw new RuntimeException(String.format("Found %s duplicate <realm> elements in <security> element", name));
                }

                String description = realmConfig.getDescription();
                String[] userPrincipalClasses = realmConfig.getUserPrincipalClassArray();

                AuthenticationType authType = realmConfig.getAuthentication();

                AuthenticationContext authenticationContext = null;
                if (authType != null) {
                    if (AuthenticationType.HttpChallengeScheme.APPLICATION_NEGOTIATE.equals(authType.getHttpChallengeScheme())) {
                        LOGGER.warn("Setting http-challenge-scheme to \"Application Negotiate\" is deprecated. Use \"Negotiate\""
                                + "instead. See \"http-challenge-scheme\" in the documentation for more information.");
                    }
                    authenticationContext = new DefaultAuthenticationContext(authType);
                }

                LoginModuleType[] loginModulesArray =
                        authType == null ?
                        new LoginModuleType[0] :
                        authType.isSetLoginModules() ?
                        authType.getLoginModules().getLoginModuleArray() :
                        new LoginModuleType[0];


                List<AppConfigurationEntry> configurationEntries = new LinkedList<>();
                for (LoginModuleType loginModule : loginModulesArray) {
                    String type = loginModule.getType();
                    String success = loginModule.getSuccess().toString();
                    Map<String, Object> options = new HashMap<>();

                    // add the GATEWAY_CONFIG_DIRECTORY to the options so it can be used from various login modules
                    // (see FileLoginModule for an example)
                    options.put(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY, configuration
                            .getProperty(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY));
                    if (LOGIN_MODULE_EXPIRING_STATE.isEnabled(configuration)) {
                        options.put(EXPIRING_STATE_OPTIONS_KEY, expiringState);
                    }

                    LoginModuleOptionsType rawOptions = loginModule.getOptions();
                    if (rawOptions != null) {
                        NodeList childNodes = rawOptions.getDomNode().getChildNodes();
                        for (int i = 0; i < childNodes.getLength(); i++) {
                            Node node = childNodes.item(i);
                            if (Node.ELEMENT_NODE == node.getNodeType()) {
                                NodeList content = node.getChildNodes();
                                options.put(node.getLocalName(), content.item(0).getNodeValue());
                            }
                        }
                    }

                    LoginModuleControlFlag controlFlag = loginModuleControlFlags.get(success);
                    if (controlFlag == null) {
                        throw new IllegalArgumentException("Unrecognized login module type: " + type);
                    }

                    if (type.startsWith(LOGIN_MODULE_TYPE_CLASS_PREFIX)) {
                        String className = type.substring(LOGIN_MODULE_TYPE_CLASS_PREFIX.length());
                        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                        try {
                            classLoader.loadClass(className);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Unable to find the login module class: " + className, e);
                        }
                        configurationEntries.add(new AppConfigurationEntry(className, controlFlag, options));
                    } else {
                        String className = getLoginModuleClass(type);
                        if (className == null) {
                            throw new IllegalArgumentException("Unrecognized login module type: " + type);
                        }
                        configurationEntries.add(new AppConfigurationEntry(className, controlFlag, options));
                    }
                }

                updateLoginModuleConfigurationEntries(securityConfig, authType, authenticationContext, configurationEntries,
                        configuration);

                realmContexts.put(name, new DefaultRealmContext(name, description, userPrincipalClasses,
                        new SingletonConfiguration(name, configurationEntries), authenticationContext));
            }
        }

        return new DefaultRealmsContext(Collections.unmodifiableMap(realmContexts));
    }

    private void updateLoginModuleConfigurationEntries(SecurityType securityConfig, AuthenticationType authType,
                                                       AuthenticationContext authenticationContext,
                                                       List<AppConfigurationEntry> configurationEntries, Properties
                                                               gatewayProperties) {

        // See KG-3362 for the logic behind injecting these configuration entries.
        final boolean authenticationContextIsPresent = authenticationContext != null;

        if (authenticationContextIsPresent) {

            String httpChallengeScheme = authenticationContext.getHttpChallengeScheme();
            if (httpChallengeScheme.startsWith(AUTH_SCHEME_APPLICATION_PREFIX)) {
                httpChallengeScheme = httpChallengeScheme.substring(AUTH_SCHEME_APPLICATION_PREFIX.length());
            }

            // Login Module Inject Rule 1: Inject Basic Login Module At Front of Chain
            if (AUTH_SCHEME_BASIC.equals(httpChallengeScheme)) {
                Map<String, String> options = new HashMap<>();
                options.put("tryFirstToken", "true");
                configurationEntries.add(0, new AppConfigurationEntry(BasicLoginModule.class.getName(),
                        LoginModuleControlFlag.OPTIONAL,
                        options));
            }

            // Login Module Inject Rule 2: Inject Negotiate Login Module at Front of Chain
            if (AUTH_SCHEME_NEGOTIATE.equals(httpChallengeScheme)) {
                Map<String, String> options = new HashMap<>();
                options.put("tryFirstToken", "true");
                configurationEntries.add(0, new AppConfigurationEntry(NegotiateLoginModule.class.getName(),
                        LoginModuleControlFlag.OPTIONAL,
                        options));
            }

            //Login Module Inject Rule 4: Inject Timeout Module at Front of Chain
            if (authType.isSetSessionTimeout() && authType.getSessionTimeout() != null) {
                Map<String, String> options = new HashMap<>();
                if (authType.isSetSessionTimeout()) {
                    options.put("session-timeout", resolveTimeIntervalValue(authType.getSessionTimeout()));
                }
                configurationEntries.add(0, new AppConfigurationEntry(TimeoutLoginModule.class.getName(),
                        LoginModuleControlFlag.OPTIONAL,
                        options));
            }

        }
    }

    private String resolveTimeIntervalValue(String value) {
        final long l = Utils.parseTimeInterval(value, TimeUnit.SECONDS, 0);
        if (l == 0) {
            return null;
        }
        return String.valueOf(l);
    }

    // NOTE: Code between the previous and next methods was moved from here to SecurityContextResolver
    //       (any changes pulled in from merges should be applied to that class)

    private Map<String, Object> resolveDependencyContext() {
        Map<String, Object> dependencyContextMap = new HashMap<>();

        ServiceLoader<DependencyContext> dependencyContextLoader = ServiceLoader.load(DependencyContext.class);
        for (DependencyContext context : dependencyContextLoader) {
            dependencyContextMap.put(context.getName(), context);
        }

        return dependencyContextMap;
    }

    private String getLoginModuleClass(String loginModuleType) {
        String loginModuleClassName = loginModuleClassNames.get(loginModuleType);
        if (loginModuleClassName == null) {
            loginModuleClassName = findLoginModuleClass(loginModuleType);
            loginModuleClassNames.put(loginModuleType, loginModuleClassName);
        }
        return loginModuleClassName;
    }

    private String findLoginModuleClass(String loginModuleType) {
        String packageName = Gateway.class.getPackage().getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("META-INF/services/" + packageName.replace('.', '/') + "/loginModule/"
                + loginModuleType);
        if (resource == null) {
            throw new IllegalArgumentException("Unrecognized login module type: " + loginModuleType);
        } else {
            try {
                Properties properties = new Properties();
                properties.load(resource.openStream());
                String className = properties.getProperty("class");
                if (className != null) {
                    return className;
                }
                throw new IllegalArgumentException("Unrecognized login module type: " + loginModuleType);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unrecognized login module type: " + loginModuleType, e);
            }
        }
    }

    private DefaultSchemeConfig findSchemeConfig(String schemeName) {
        String packageName = Gateway.class.getPackage().getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("META-INF/services/" + packageName.replace('.', '/') + "/scheme/" + schemeName);
        if (resource != null) {
            try {
                Properties properties = new Properties();
                properties.load(resource.openStream());

                String transportName = properties.getProperty("transport");
                String defaultPort = properties.getProperty("port");

                DefaultSchemeConfig schemeConfig = new DefaultSchemeConfig();
                schemeConfig.setName(schemeName);
                schemeConfig.setTransportName(transportName);
                if (defaultPort != null) {
                    schemeConfig.setDefaultPort(Integer.parseInt(defaultPort));
                }

                return schemeConfig;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private Map<String, DefaultTransportContext> resolveTransports(TransportFactory transportFactory) throws Exception {
        for (String transportName : transportFactory.getTransportNames()) {
            Transport transport = transportFactory.getTransport(transportName);
            DefaultTransportContext transportContext =
                    new DefaultTransportContext(transportName, transport);
            transportContextsByName.put(transportName, transportContext);
        }
        return transportContextsByName;
    }

//    private SessionInitializerFactory resolveSessionInitializerFactory() {
//        SessionInitializerFactoryImpl factory = new SessionInitializerFactoryImpl();
//        // FIXME: Do whatever needed here to get the list of initializer classes into the factory instance.
//        return factory;
//    }


    /**
     * Gross method to tell us if a given service type supports using 'accept's. XXX This needs to be fixed later!
     *
     * @param serviceType
     * @return
     */
    private boolean supportsAccepts(String serviceType) {
        return !serviceType.equals("management.jmx") &&
                !serviceType.equals("$management.jmx$");
    }

    /**
     * Gross method to tell us if a given service type supports using 'connect's. XXX This needs to be fixed later!
     *
     * @param serviceType
     * @return
     */
    private boolean supportsConnects(String serviceType) {
        return !serviceType.equals("jms") &&
                !serviceType.equals("echo") &&
                !serviceType.equals("management.jmx") &&
                !serviceType.equals("$management.jmx$") &&
                !serviceType.equals("management.snmp") &&
                !serviceType.equals("$management.snmp$") &&
                !serviceType.equals("directory");
    }

    /**
     * Gross method to tell us if a given service type supports using MIME mappings. For now, the only one that really does is
     * the HttpDirectoryService. XXX This may need to be fixed later!
     *
     * @param serviceType
     * @return
     */
    private boolean supportsMimeMappings(String serviceType) {
        return serviceType.equals("directory");
    }


    private static class SingletonConfiguration extends Configuration {

        private final Map<String, AppConfigurationEntry[]> configurationEntries;

        public SingletonConfiguration(String applicationName, Collection<AppConfigurationEntry> configurationEntries) {
            AppConfigurationEntry[] array = new AppConfigurationEntry[configurationEntries.size()];
            this.configurationEntries = Collections.singletonMap(applicationName,
                    configurationEntries.toArray(array));
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String configName) {
            return configurationEntries.get(configName);
        }

        @SuppressWarnings("unused")
        public void setAppConfigurationEntry(String configName, AppConfigurationEntry[] entry) {
            configurationEntries.put(configName, entry);
        }
    }

    private void injectResources(Collection<ServiceContext> services,
                                 BridgeServiceFactory bridgeServiceFactory,
                                 Map<String, Object> dependencyContexts,
                                 Map<String, Object> injectables) {

        // add all of the transport acceptors, connectors and extensions
        injectables = bridgeServiceFactory.getTransportFactory().injectResources(injectables);

        // inject services
        for (ServiceContext serviceContext : services) {
            injectResources(serviceContext.getService(), injectables);
        }

        // in case any of the DependencyContexts have dependencies on each other,
        // or the other resources added to the map, inject resources for them as well.
        for (Object obj : dependencyContexts.values()) {
            injectResources(obj, injectables);
        }
    }

    // TODO: should do more checking here to verify method has the right number
    // of arguments and of the right type
    // TODO: do a better job of injection by trying to match method argument types of anything with @Resource, from a list of
    // known resources
    private void injectResources(Object target, Map<String, Object> values) {
        if (target == null) {
            return;
        }

        Class<?> clazz = target.getClass();

        for (Method method : clazz.getMethods()) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null) {
                String name = resource.name();
                Object val = values.get(name);
                try {
                    method.invoke(target, val);
                } catch (Exception e) {
                    LOGGER.warn("Error while injecting named " + name + " resource", e);
                }
            }
        }
    }

    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul> <li>the host part of the authority
     * lower-case since URI semantics dictate that hostnames are case insensitive <li>(optionally, NOT appropriate for Origin
     * headers) the path part set to "/" if there was no path in the input URI (this conforms to the WebSocket and HTTP protocol
     * specifications and avoids us having to do special handling for path throughout the server code). </ul>
     *
     * @param uriString        the URI to canonicalize, in string form
     * @param canonicalizePath if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally) trailing / added, or null if the uri is null
     * @throws IllegalArgumentException if the uriString is not valid syntax
     */
    public static String getCanonicalURI(String uriString, boolean canonicalizePath) {
        if ((uriString != null) && !"".equals(uriString)) {
            return getCanonicalizedURI(uriString, canonicalizePath);
        }
        return null;
    }

    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul> <li>the host part of the authority
     * lower-case since URI semantics dictate that hostnames are case insensitive <li>(optionally, NOT appropriate for Origin
     * headers) the path part set to "/" except for tcp uris if there was no path in the input URI (this conforms to the
     * WebSocket and HTTP protocol specifications and avoids us having to do special handling for path throughout the server
     * code). </ul>
     *
     * @param uri              the URI to canonicalize
     * @param canonicalizePath if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally if not tcp) trailing / added, or null if the
     * uri is null
     * @throws IllegalArgumentException if the uri is not valid syntax
     */
    public static String getCanonicalizedURI(String uri, boolean canonicalizePath) {
        String canonicalURI = uri;
        if (uri != null) {
            String host = getHost(uri);
            String path = getPath(uri);
            final boolean emptyPath = "".equals(path);
            final boolean noPathToCanonicalize = canonicalizePath && (path == null || emptyPath);
            final boolean trailingSlashPath = "/".equals(path);
            final String scheme = getScheme(uri);
            final boolean pathlessScheme = "ssl".equals(scheme) || "tcp".equals(scheme) || "pipe".equals(scheme)
                    || "udp".equals(scheme) || "mux".equals(scheme);
            final boolean trailingSlashWithPathlessScheme = trailingSlashPath && pathlessScheme;
            String newPath = trailingSlashWithPathlessScheme ? "" :
                             noPathToCanonicalize ? (pathlessScheme ? null : "/") : null;
            if (((host != null) && !host.equals(host.toLowerCase())) || newPath != null) {
                path = newPath == null ? path : newPath;
                try {
                    canonicalURI = buildURIAsString(scheme, getUserInfo(uri), host == null ?
                            null : host.toLowerCase(), getPort(uri), path, getQuery(uri), getFragment(uri));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Invalid URI: " + uri + " in Gateway configuration file", ex);
                }
            }
        }
        return canonicalURI;
    }

    public void addInjectable(String key, Object value) {
        this.injectables.put(key, value);
    }

    public Map<String, Object> getInjectables() {
        return injectables;
    }

}
