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
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.MAXIMUM_REDIRECTS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_TIMEOUT;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.LOGIN_CONTEXT_FACTORY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.ORIGIN_SECURITY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_AUTHENTICATION_COOKIE_NAMES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_AUTHENTICATION_HEADER_NAMES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_AUTHENTICATION_PARAMETER_NAMES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_AUTHORIZATION_MODE;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_CHALLENGE_SCHEME;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_DESCRIPTION;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_NAME;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALM_USER_PRINCIPAL_CLASSES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REQUIRED_ROLES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.SERVER_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.USER_AGENT_HEADER_ENABLED;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.HOST_HEADER_ENABLED;
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

import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.LoginContextFactory;

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
        
        String realmName = (String) optionsByName.remove(REALM_NAME.name());
        if (realmName != null) {
            options.setOption(REALM_NAME, realmName);
        }

        String realmAuthorizationMode = (String) optionsByName.remove(REALM_AUTHORIZATION_MODE.name());
        if (realmAuthorizationMode != null) {
            options.setOption(REALM_AUTHORIZATION_MODE, realmAuthorizationMode);
        }

        String realmChallengeScheme = (String) optionsByName.remove(REALM_CHALLENGE_SCHEME.name());
        if (realmChallengeScheme != null) {
            options.setOption(REALM_CHALLENGE_SCHEME, realmChallengeScheme);
        }

        String realmDescription = (String) optionsByName.remove(REALM_DESCRIPTION.name());
        if (realmDescription != null) {
            options.setOption(REALM_DESCRIPTION, realmDescription);
        }

        String[] realmAuthenticationHeaderNames = (String[]) optionsByName.remove(REALM_AUTHENTICATION_HEADER_NAMES.name());
        if (realmAuthenticationHeaderNames != null) {
            options.setOption(REALM_AUTHENTICATION_HEADER_NAMES, realmAuthenticationHeaderNames);
        }

        String[] realmAuthenticationParameterNames = (String[]) optionsByName.remove(REALM_AUTHENTICATION_PARAMETER_NAMES.name());
        if (realmAuthenticationParameterNames != null) {
            options.setOption(REALM_AUTHENTICATION_PARAMETER_NAMES, realmAuthenticationParameterNames);
        }

        String[] realmAuthenticationCookieNames = (String[]) optionsByName.remove(REALM_AUTHENTICATION_COOKIE_NAMES.name());
        if (realmAuthenticationCookieNames != null) {
            options.setOption(REALM_AUTHENTICATION_COOKIE_NAMES, realmAuthenticationCookieNames);
        }

        LoginContextFactory loginContextFactory = (LoginContextFactory) optionsByName.remove(LOGIN_CONTEXT_FACTORY.name());
        if (loginContextFactory != null) {
            options.setOption(LOGIN_CONTEXT_FACTORY, loginContextFactory);
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

        Boolean userAgentHeaderEnabled = (Boolean) optionsByName.remove(USER_AGENT_HEADER_ENABLED.name());
        if (userAgentHeaderEnabled != null) {
            options.setOption(USER_AGENT_HEADER_ENABLED, userAgentHeaderEnabled);
        }

        Boolean hostHeaderEnabled = (Boolean) optionsByName.remove(HOST_HEADER_ENABLED.name());
        if (hostHeaderEnabled != null) {
            options.setOption(HOST_HEADER_ENABLED, hostHeaderEnabled);
        }

        Collection<Class<? extends Principal>> realmUserPrincipalClasses = (Collection<Class<? extends Principal>>) optionsByName.remove(REALM_USER_PRINCIPAL_CLASSES.name());
        if (realmUserPrincipalClasses != null) {
            options.setOption(REALM_USER_PRINCIPAL_CLASSES, realmUserPrincipalClasses);
        }

        IdentityResolver httpIdentityResolver = (IdentityResolver) optionsByName.remove(IDENTITY_RESOLVER.name());
        if (httpIdentityResolver != null) {
            options.setOption(IDENTITY_RESOLVER, httpIdentityResolver);
        }
        else {
            if (realmUserPrincipalClasses != null && realmUserPrincipalClasses.size() > 0) {
                httpIdentityResolver = new HttpIdentityResolver(realmUserPrincipalClasses);
                options.setOption(IDENTITY_RESOLVER, httpIdentityResolver);
            }
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
        address.setOption0(REALM_NAME, options.getOption(REALM_NAME));
        address.setOption0(REALM_AUTHORIZATION_MODE, options.getOption(REALM_AUTHORIZATION_MODE));
        address.setOption0(REALM_CHALLENGE_SCHEME, options.getOption(REALM_CHALLENGE_SCHEME));
        address.setOption0(REALM_DESCRIPTION, options.getOption(REALM_DESCRIPTION));
        address.setOption0(REALM_AUTHENTICATION_HEADER_NAMES, options.getOption(REALM_AUTHENTICATION_HEADER_NAMES));
        address.setOption0(REALM_AUTHENTICATION_PARAMETER_NAMES, options.getOption(REALM_AUTHENTICATION_PARAMETER_NAMES));
        address.setOption0(REALM_AUTHENTICATION_COOKIE_NAMES, options.getOption(REALM_AUTHENTICATION_COOKIE_NAMES));
        address.setOption0(LOGIN_CONTEXT_FACTORY, options.getOption(LOGIN_CONTEXT_FACTORY));
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
        address.setOption0(REALM_USER_PRINCIPAL_CLASSES, options.getOption(REALM_USER_PRINCIPAL_CLASSES));
        if (address.getOption(IDENTITY_RESOLVER) == null) {
             Collection<Class<? extends Principal>> realmUserPrincipalClasses = address.getOption(REALM_USER_PRINCIPAL_CLASSES);
             if (realmUserPrincipalClasses != null && realmUserPrincipalClasses.size() > 0) {
                 IdentityResolver httpIdentityResolver = new HttpIdentityResolver(realmUserPrincipalClasses);
                 address.setIdentityResolver(IDENTITY_RESOLVER, httpIdentityResolver);
             }
        }
    }

}
