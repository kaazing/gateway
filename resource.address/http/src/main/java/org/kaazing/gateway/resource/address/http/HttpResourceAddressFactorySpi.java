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
package org.kaazing.gateway.resource.address.http;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.IDENTITY_RESOLVER;
import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;
import static org.kaazing.gateway.resource.address.ResourceFactories.keepAuthorityOnly;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.AUTHENTICATION_CONNECT;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.AUTHENTICATION_IDENTIFIER;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.BALANCE_ORIGINS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.ENCRYPTION_KEY_ALIAS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.GATEWAY_ORIGIN_SECURITY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_CONNECTIONS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_TIMEOUT;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.MAXIMUM_REDIRECTS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.MAX_AUTHENTICATION_ATTEMPTS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.ORIGIN_SECURITY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALMS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REQUIRED_ROLES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.SERVER_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.USER_AGENT_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.HOST_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.DATE_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.SERVICE_DOMAIN;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.TEMP_DIRECTORY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.TRANSPORT_NAME;

import java.io.File;
import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.security.CrossSiteConstraintContext;

public class HttpResourceAddressFactorySpi extends ResourceAddressFactorySpi<HttpResourceAddress> {

    public static final int SCHEME_PORT = 80;
    
    private static final String SCHEME_NAME = "http";
    private static final ResourceFactory TRANSPORT_FACTORY = keepAuthorityOnly("tcp");

    private static final String PROTOCOL_NAME = "http/1.1";

    private static final Map<String, List<ResourceFactory>> RESOURCE_FACTORIES_BY_KEY
            = new HashMap<>();
    
    static {
        // go backwards so we can set alternate addresses correctly
        List<ResourceFactory> insecureAlternateResourceFactories = Collections.singletonList(
                changeSchemeOnly("httpxe")
        );

        List<ResourceFactory> secureAlternateResourceFactories = Collections.singletonList(
                changeSchemeOnly("httpxe+ssl")
        );

        RESOURCE_FACTORIES_BY_KEY.put("wse/1.0",
                                      insecureAlternateResourceFactories);
        RESOURCE_FACTORIES_BY_KEY.put("wse/1.0 secure",
                                      secureAlternateResourceFactories);
    }

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    protected int getSchemePort() {
        return SCHEME_PORT;
    }

    @Override
    protected String getTransportName() {
        return TRANSPORT_NAME;
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return TRANSPORT_FACTORY;
    }

    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void parseNamedOptions0(String location, ResourceOptions options,
                                      Map<String, Object> optionsByName) {

        Boolean keepAlive = (Boolean) optionsByName.remove(KEEP_ALIVE.name());
        if (keepAlive != null) {
            options.setOption(KEEP_ALIVE, keepAlive);
        }

        Object maxRedirects = optionsByName.remove(MAXIMUM_REDIRECTS.name());
        if (maxRedirects != null) {
            options.setOption(MAXIMUM_REDIRECTS, maxRedirects instanceof String ? Integer.parseInt((String) maxRedirects) : (Integer) maxRedirects);
        }

        Integer keepAliveTimeout = (Integer) optionsByName.remove(KEEP_ALIVE_TIMEOUT.name());
        if (keepAliveTimeout != null) {
            options.setOption(KEEP_ALIVE_TIMEOUT, keepAliveTimeout);
        }

        Integer keepAliveConnections = (Integer) optionsByName.remove(KEEP_ALIVE_CONNECTIONS.name());
        if (keepAliveConnections != null) {
            options.setOption(KEEP_ALIVE_CONNECTIONS, keepAliveConnections);
        }
        
        String[] requiredRoles = (String[]) optionsByName.remove(REQUIRED_ROLES.name());
        if (requiredRoles != null) {
            options.setOption(REQUIRED_ROLES, requiredRoles);
        }

        String authenticationConnect = (String) optionsByName.remove(AUTHENTICATION_CONNECT.name());
        if (authenticationConnect != null) {
            options.setOption(AUTHENTICATION_CONNECT, authenticationConnect);
        }

        String authenticationIdentifier = (String) optionsByName.remove(AUTHENTICATION_IDENTIFIER.name());
        if (authenticationIdentifier != null) {
            options.setOption(AUTHENTICATION_IDENTIFIER, authenticationIdentifier);
        }

        String encryptionKeyAlias = (String) optionsByName.remove(ENCRYPTION_KEY_ALIAS.name());
        if (encryptionKeyAlias != null) {
            options.setOption(ENCRYPTION_KEY_ALIAS, encryptionKeyAlias);
        }

        String serviceDomain = (String) optionsByName.remove(SERVICE_DOMAIN.name());
        if (serviceDomain != null) {
            options.setOption(SERVICE_DOMAIN, serviceDomain);
        }

        Set<HttpInjectableHeader> injectableHeaders = (Set<HttpInjectableHeader>) optionsByName.remove(INJECTABLE_HEADERS.name());
        if (injectableHeaders != null) {
            options.setOption(INJECTABLE_HEADERS, injectableHeaders);
        }

        Map<String, ? extends CrossSiteConstraintContext> acceptConstraints = (Map<String, ? extends CrossSiteConstraintContext>) optionsByName.remove(ORIGIN_SECURITY.name());
        if (acceptConstraints != null) {
            options.setOption(ORIGIN_SECURITY, new HttpOriginSecurity(acceptConstraints));
        }
        
        File tempDirectory = (File) optionsByName.remove(TEMP_DIRECTORY.name());
        if (tempDirectory != null) {
            options.setOption(TEMP_DIRECTORY, tempDirectory);
        }

        List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI = (List<Map<String, Map<String, CrossSiteConstraintContext>>>) optionsByName.remove(GATEWAY_ORIGIN_SECURITY.name());
        if (authorityToSetOfAcceptConstraintsByURI != null) {
            options.setOption(GATEWAY_ORIGIN_SECURITY, new GatewayHttpOriginSecurity(authorityToSetOfAcceptConstraintsByURI));
        }

        Collection<String> balanceOrigin = (Collection<String>) optionsByName.remove(BALANCE_ORIGINS.name());
        if (balanceOrigin != null) {
            options.setOption(BALANCE_ORIGINS, balanceOrigin);
        }

        Boolean serverHeaderEnabled = (Boolean) optionsByName.remove(SERVER_HEADER_ENABLED.name());
        if (serverHeaderEnabled != null) {
            options.setOption(SERVER_HEADER_ENABLED, serverHeaderEnabled);
        }
        
        Boolean dateHeaderEnabled = (Boolean) optionsByName.remove(DATE_HEADER_ENABLED.name());
        if (serverHeaderEnabled != null) {
            options.setOption(DATE_HEADER_ENABLED, dateHeaderEnabled);
        }

        Boolean userAgentHeaderEnabled = (Boolean) optionsByName.remove(USER_AGENT_HEADER_ENABLED.name());
        if (userAgentHeaderEnabled != null) {
            options.setOption(USER_AGENT_HEADER_ENABLED, userAgentHeaderEnabled);
        }

        Boolean hostHeaderEnabled = (Boolean) optionsByName.remove(HOST_HEADER_ENABLED.name());
        if (hostHeaderEnabled != null) {
            options.setOption(HOST_HEADER_ENABLED, hostHeaderEnabled);
        }

        Object maxAuthenticationAttempts = optionsByName.remove(MAX_AUTHENTICATION_ATTEMPTS.name());
        if (maxAuthenticationAttempts != null) {
            if (maxAuthenticationAttempts instanceof String) {
                maxAuthenticationAttempts = Integer.parseInt((String) maxAuthenticationAttempts);
            }
            options.setOption(MAX_AUTHENTICATION_ATTEMPTS, (Integer) maxAuthenticationAttempts);
        }

        HttpRealmInfo[] realms = (HttpRealmInfo[]) optionsByName.remove(REALMS.name());
        if (realms != null) {
            options.setOption(REALMS, realms);
        }

        IdentityResolver httpIdentityResolver = (IdentityResolver) optionsByName.remove(IDENTITY_RESOLVER.name());
        if (httpIdentityResolver != null) {
            options.setOption(IDENTITY_RESOLVER, httpIdentityResolver);
        } else {
            Collection<Class<? extends Principal>> userPrincipalClasses =
                    getUserPrincipalClasses((HttpRealmInfo[]) optionsByName.remove(REALMS.name()));
            if (userPrincipalClasses != null && !userPrincipalClasses.isEmpty()) {
                httpIdentityResolver = new HttpIdentityResolver(userPrincipalClasses);
                options.setOption(IDENTITY_RESOLVER, httpIdentityResolver);
            }
        }
    }

    private Collection<Class<? extends Principal>> getUserPrincipalClasses(HttpRealmInfo[] realms) {
        if (realms != null && realms.length > 0) {
            if (realms.length == 1) {
                return realms[0].getUserPrincipleClasses();
            } else {
                return Arrays.stream(realms).map(r -> r.getUserPrincipleClasses()).filter(upc -> upc != null)
                        .flatMap(upc -> upc.stream()).collect(Collectors.toList());
            }
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    protected void setAlternateOption(final String location,
                                      ResourceOptions options,
                                      Map<String, Object> optionsByName) {
        String key = options.getOption(HttpResourceAddress.NEXT_PROTOCOL);
        String scheme = URIUtils.getScheme(location);
        final boolean secureScheme = "https".equals(scheme) || scheme.contains("+ssl");
        if (key != null && secureScheme) {
            key = key + " secure";
        }

        List<ResourceFactory> resourceFactories =
                RESOURCE_FACTORIES_BY_KEY.get(key);

        if (resourceFactories != null &&
                !resourceFactories.isEmpty() &&
                getResourceAddressFactory() != null) {

            // create alternate addresses

            HttpResourceAddress alternateAddress = null;
            for (ResourceFactory resourceFactory: resourceFactories) {
                alternateAddress = newResourceAddressWithAlternate(
                        resourceFactory.createURI(location),
                        getNewOptionsByName(options, optionsByName),
                        alternateAddress);
            }

            // save the alternate chain into this address.
            options.setOption(ResourceAddress.ALTERNATE, alternateAddress);
        }
    }

    @Override
    protected HttpResourceAddress newResourceAddress0(String original, String location) {

        String host = URIUtils.getHost(location);
        int port = URIUtils.getPort(location);
        String path = URIUtils.getPath(location);

        if (host == null) {
            throw new IllegalArgumentException(format("Missing host in URI: %s", location));
        }

        if (port == -1) {
            throw new IllegalArgumentException(format("Missing port in URI: %s", location));
        }
        
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException(format("Missing path in URI: %s", location));
        }
        
        URI uriLocation = URI.create(location);
        return new HttpResourceAddress(this, original, uriLocation);
    }

    @Override
    protected void setOptions(HttpResourceAddress address, ResourceOptions options, Object qualifier) {

        super.setOptions(address, options, qualifier);

        address.setOption0(KEEP_ALIVE, options.getOption(KEEP_ALIVE));
        address.setOption0(MAXIMUM_REDIRECTS,options.getOption(MAXIMUM_REDIRECTS));
        address.setOption0(KEEP_ALIVE_TIMEOUT, options.getOption(KEEP_ALIVE_TIMEOUT));
        address.setOption0(KEEP_ALIVE_CONNECTIONS, options.getOption(KEEP_ALIVE_CONNECTIONS));
        address.setOption0(REQUIRED_ROLES, options.getOption(REQUIRED_ROLES));
        address.setOption0(INJECTABLE_HEADERS, options.getOption(INJECTABLE_HEADERS));
        address.setOption0(ORIGIN_SECURITY, options.getOption(ORIGIN_SECURITY));
        address.setOption0(TEMP_DIRECTORY, options.getOption(TEMP_DIRECTORY));
        address.setOption0(GATEWAY_ORIGIN_SECURITY, options.getOption(GATEWAY_ORIGIN_SECURITY));
        address.setOption0(BALANCE_ORIGINS, options.getOption(BALANCE_ORIGINS));
        address.setOption0(AUTHENTICATION_CONNECT, options.getOption(AUTHENTICATION_CONNECT));
        address.setOption0(AUTHENTICATION_IDENTIFIER, options.getOption(AUTHENTICATION_IDENTIFIER));
        address.setOption0(ENCRYPTION_KEY_ALIAS, options.getOption(ENCRYPTION_KEY_ALIAS));
        address.setOption0(SERVICE_DOMAIN, options.getOption(SERVICE_DOMAIN));
        address.setOption0(SERVER_HEADER_ENABLED, options.getOption(SERVER_HEADER_ENABLED));
        address.setOption0(USER_AGENT_HEADER_ENABLED, options.getOption(USER_AGENT_HEADER_ENABLED));
        address.setOption0(HOST_HEADER_ENABLED, options.getOption(HOST_HEADER_ENABLED));
        address.setOption0(DATE_HEADER_ENABLED, options.getOption(DATE_HEADER_ENABLED));
        address.setOption0(MAX_AUTHENTICATION_ATTEMPTS, options.getOption(MAX_AUTHENTICATION_ATTEMPTS));
        address.setOption0(REALMS, options.getOption(REALMS));
        if (address.getOption(IDENTITY_RESOLVER) == null) {
             Collection<Class<? extends Principal>> realmUserPrincipalClasses = getUserPrincipalClasses(address.getOption(REALMS));
             if (realmUserPrincipalClasses != null && !realmUserPrincipalClasses.isEmpty()) {
                 IdentityResolver httpIdentityResolver = new HttpIdentityResolver(realmUserPrincipalClasses);
                 address.setIdentityResolver(IDENTITY_RESOLVER, httpIdentityResolver);
             }
        }
    }

}
