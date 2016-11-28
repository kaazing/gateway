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

import java.io.File;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOption;

public final class HttpResourceAddress extends ResourceAddress {
	
	private static final long serialVersionUID = 1L;

    static final int DEFAULT_HTTP_KEEPALIVE_CONNECTIONS = 2;

	static final String TRANSPORT_NAME = "http";

    public static final ResourceOption<Boolean> KEEP_ALIVE = new HttpKeepAliveOption();
    public static final ResourceOption<Integer> MAXIMUM_REDIRECTS = new HttpMaxRedirectOption();
    public static final ResourceOption<Integer> KEEP_ALIVE_TIMEOUT = new HttpKeepAliveTimeoutOption();
    public static final ResourceOption<Integer> KEEP_ALIVE_CONNECTIONS = new HttpKeepAliveConnectionsOption();

    public static final ResourceOption<String[]> REQUIRED_ROLES = new HttpRequiredRolesOption();
    public static final ResourceOption<Set<HttpInjectableHeader>> INJECTABLE_HEADERS = new HttpInjectableHeadersOption();
    public static final ResourceOption<HttpOriginSecurity> ORIGIN_SECURITY = new HttpOriginSecurityOption();
    public static final ResourceOption<File> TEMP_DIRECTORY = new HttpTempDirectoryOption();
    public static final ResourceOption<GatewayHttpOriginSecurity> GATEWAY_ORIGIN_SECURITY = new GatewayHttpOriginSecurityOption();
    public static final ResourceOption<Collection<String>> BALANCE_ORIGINS = new HttpBalanceOriginsOption();

    public static final ResourceOption<String> AUTHENTICATION_CONNECT = new AuthenticationConnectOption();
    public static final ResourceOption<String> AUTHENTICATION_IDENTIFIER = new AuthenticationIdentifierOption();
    public static final ResourceOption<String> ENCRYPTION_KEY_ALIAS = new EncryptionKeyAliasOption();
    public static final ResourceOption<String> SERVICE_DOMAIN = new ServiceDomainOption();
    public static final HttpResourceOption<Boolean> SERVER_HEADER_ENABLED = new HttpServerHeaderOption();

    public static final ResourceOption<Integer> MAX_AUTHENTICATION_ATTEMPTS = new MaxAuthenticationAttemptsOption();

    public static final ResourceOption<HttpRealmInfo[]> REALMS = new HttpRealmsOption();

    private Boolean serverHeaderEnabled = SERVER_HEADER_ENABLED.defaultValue();
    private Boolean keepAlive = KEEP_ALIVE.defaultValue();
    private Integer httpMaxRedirects = MAXIMUM_REDIRECTS.defaultValue();
    private Integer keepAliveTimeout = KEEP_ALIVE_TIMEOUT.defaultValue();
    private Integer keepAliveMaxConnections = KEEP_ALIVE_CONNECTIONS.defaultValue();
    private String[] requiredRoles = REQUIRED_ROLES.defaultValue();
    private Set<HttpInjectableHeader> injectableHeaders = INJECTABLE_HEADERS.defaultValue();
    private HttpOriginSecurity originSecurity;
    private File tempDirectory;
    private GatewayHttpOriginSecurity gatewayOriginSecurity;
    private Collection<String> balanceOrigins;
    private Integer maxAuthenticationAttempts;
    

    private String authenticationConnect;
    private String authenticationIdentifier;
    private String encryptionKeyAlias;
    private String serviceDomain;

    private HttpRealmInfo[] realms;

    private Collection<Class<? extends Principal>> realmUserPrincipalClasses;


	HttpResourceAddress(ResourceAddressFactorySpi factory, String original, URI resource) {
		super(factory, original, resource);
	}

	@Override
	@SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
		if (option instanceof HttpResourceOption) {
            HttpResourceOption httpOption = (HttpResourceOption)option;
            switch (httpOption.kind) {
                case KEEP_ALIVE:
                    return (V) keepAlive;
                case MAX_REDIRECTS:
                    return (V) httpMaxRedirects;
                case KEEP_ALIVE_TIMEOUT:
                    return (V) keepAliveTimeout;
                case KEEP_ALIVE_CONNECTIONS:
                    return (V) keepAliveMaxConnections;
                case REQUIRED_ROLES:
                    return (V) requiredRoles;
                case INJECTABLE_HEADERS:
                    return (V) injectableHeaders;
                case ORIGIN_SECURITY:
                    return (V) originSecurity;
                case TEMP_DIRECTORY:
                    return (V) tempDirectory;
                case GATEWAY_ORIGIN_SECURITY:
                    return (V) gatewayOriginSecurity;
                case BALANCE_ORIGINS:
                    return (V) balanceOrigins;
                case AUTHENTICATION_CONNECT:
                    return (V) authenticationConnect;
                case AUTHENTICATION_IDENTIFIER:
                    return (V) authenticationIdentifier;
                case ENCRYPTION_KEY_ALIAS:
                    return (V) encryptionKeyAlias;
                case SERVICE_DOMAIN:
                    return (V) serviceDomain;
                case SERVER_HEADER:
                    return (V) serverHeaderEnabled;
                case MAX_AUTHENTICATION_ATTEMPTS:
                    return (V) maxAuthenticationAttempts;
                case REALM_USER_PRINCIPAL_CLASSES:
                    return (V) realmUserPrincipalClasses;
                case REALMS:
                    return (V) realms;
            }
        }

		return super.getOption0(option);
	}

    @Override
	@SuppressWarnings({ "unchecked" })
    protected <V> void setOption0(ResourceOption<V> option, V value) {
        if (option instanceof HttpResourceOption) {
            HttpResourceOption httpOption = (HttpResourceOption) option;
            switch (httpOption.kind) {
                case KEEP_ALIVE:
                    keepAlive = (Boolean) value;
                    return;
                case MAX_REDIRECTS:
                    httpMaxRedirects = value instanceof String ? Integer.parseInt((String) value) : (Integer) value;
                    return;
                case KEEP_ALIVE_TIMEOUT:
                    keepAliveTimeout = (Integer) value;
                    return;
                case KEEP_ALIVE_CONNECTIONS:
                    keepAliveMaxConnections = (Integer) value;
                    return;
                case REQUIRED_ROLES:
                    requiredRoles = (String[]) value;
                    return;
                case AUTHENTICATION_CONNECT:
                    authenticationConnect = (String) value;
                    return;
                case AUTHENTICATION_IDENTIFIER:
                    authenticationIdentifier = (String) value;
                    return;
                case ENCRYPTION_KEY_ALIAS:
                    encryptionKeyAlias = (String) value;
                    return;
                case SERVICE_DOMAIN:
                    serviceDomain = (String) value;
                    return;
                case INJECTABLE_HEADERS:
                    injectableHeaders = (Set<HttpInjectableHeader>) value;
                    return;
                case ORIGIN_SECURITY:
                    originSecurity = (HttpOriginSecurity) value;
                    return;
                case TEMP_DIRECTORY:
                    tempDirectory = (File) value;
                    return;
                case GATEWAY_ORIGIN_SECURITY:
                    gatewayOriginSecurity = (GatewayHttpOriginSecurity) value;
                    return;
                case BALANCE_ORIGINS:
                    balanceOrigins = (Collection<String>) value;
                    return;
                case SERVER_HEADER:
                    serverHeaderEnabled = (Boolean) value;
                    return;
                case REALM_USER_PRINCIPAL_CLASSES:
                    realmUserPrincipalClasses = (Collection<Class<? extends Principal>>) value;
                    return;
                case MAX_AUTHENTICATION_ATTEMPTS:
                    maxAuthenticationAttempts = (Integer) value;
                    return;
                case REALMS:
                    realms = (HttpRealmInfo[]) value;
                    return;
            }
        }

        super.setOption0(option, value);
    }

    /**
     * Default access method allowing to set an identity resolver (should allow usage from within the same package)
     * @param identityResolverOption
     * @param identityResolver
     */
    void setIdentityResolver(DefaultResourceOption<IdentityResolver> identityResolverOption,
                             IdentityResolver identityResolver) {
        super.setOption0(identityResolverOption, identityResolver);
    }

    public static class HttpResourceOption<T> extends ResourceOption<T> {

        protected enum Kind {
            //@formatter:off
            KEEP_ALIVE,
            KEEP_ALIVE_TIMEOUT,
            KEEP_ALIVE_CONNECTIONS,
            REQUIRED_ROLES,
            INJECTABLE_HEADERS,
            ORIGIN_SECURITY,
            TEMP_DIRECTORY,
            GATEWAY_ORIGIN_SECURITY,
            BALANCE_ORIGINS,
            AUTHENTICATION_CONNECT,
            AUTHENTICATION_IDENTIFIER,
            ENCRYPTION_KEY_ALIAS,
            SERVICE_DOMAIN,
            SERVER_HEADER,
            REALM_USER_PRINCIPAL_CLASSES,
            MAX_REDIRECTS,
            MAX_AUTHENTICATION_ATTEMPTS,
            REALMS;
          //@formatter:on
        }

        private static final Map<String, ResourceOption<?>> OPTION_NAMES = new HashMap<>();

        private final Kind kind;

        private HttpResourceOption(Kind kind, String name) {
            this(kind, name, null);
        }

        private HttpResourceOption(Kind kind, String name, T defaultValue) {
            super(OPTION_NAMES, name, defaultValue);
            this.kind = kind;
        }
    }

    private static final class HttpKeepAliveTimeoutOption extends HttpResourceOption<Integer> {
        private HttpKeepAliveTimeoutOption() {
            super(Kind.KEEP_ALIVE_TIMEOUT, "keepAliveTimeout", 30);
        }
    }

    private static final class HttpKeepAliveConnectionsOption extends HttpResourceOption<Integer> {
        private HttpKeepAliveConnectionsOption() {
            super(Kind.KEEP_ALIVE_CONNECTIONS, "keepalive.connections", DEFAULT_HTTP_KEEPALIVE_CONNECTIONS);
        }
    }

    private static final class HttpMaxRedirectOption extends HttpResourceOption<Integer> {
        private HttpMaxRedirectOption() {
            super(Kind.MAX_REDIRECTS, "maximum.redirects", 0);
        }
    }

    private static final class HttpKeepAliveOption extends HttpResourceOption<Boolean> {
        private HttpKeepAliveOption() {
            super(Kind.KEEP_ALIVE, "keepAlive", Boolean.TRUE);
        }
    }
    
    private static final class HttpRequiredRolesOption extends HttpResourceOption<String[]> {
        private HttpRequiredRolesOption() {
            super(Kind.REQUIRED_ROLES, "requiredRoles", new String[0]);
        }
    }

    private static final class HttpInjectableHeadersOption extends HttpResourceOption<Set<HttpInjectableHeader>> {
        private HttpInjectableHeadersOption() {
            super(Kind.INJECTABLE_HEADERS, "injectableHeaders", EnumSet.allOf(HttpInjectableHeader.class));
        }
    }


    private static final class HttpOriginSecurityOption extends HttpResourceOption<HttpOriginSecurity> {
        private HttpOriginSecurityOption() {
            super(Kind.ORIGIN_SECURITY, "originSecurity");
        }
    }
    
    private static final class HttpTempDirectoryOption extends HttpResourceOption<File> {
        private HttpTempDirectoryOption() {
            super(Kind.TEMP_DIRECTORY, "tempDirectory");
        }
    }
    
    private static final class GatewayHttpOriginSecurityOption extends HttpResourceOption<GatewayHttpOriginSecurity> {
        private GatewayHttpOriginSecurityOption() {
            super(Kind.GATEWAY_ORIGIN_SECURITY, "gatewayHttpOriginSecurity");
        }
    }

    private static final class HttpBalanceOriginsOption extends HttpResourceOption<Collection<String>> {
        private HttpBalanceOriginsOption() {
            super(Kind.BALANCE_ORIGINS, "balanceOrigins");
        }
    }

    private static final class AuthenticationConnectOption extends HttpResourceOption<String> {
        private AuthenticationConnectOption() {
            super(Kind.AUTHENTICATION_CONNECT, "authenticationConnect");
        }
    }

    private static final class AuthenticationIdentifierOption extends HttpResourceOption<String> {
        private AuthenticationIdentifierOption() {
            super(Kind.AUTHENTICATION_IDENTIFIER, "authenticationIdentifier");
        }
    }

    private static final class EncryptionKeyAliasOption extends HttpResourceOption<String> {
        private EncryptionKeyAliasOption() {
            super(Kind.ENCRYPTION_KEY_ALIAS, "encryptionKeyAlias");
        }
    }

    private static final class ServiceDomainOption extends HttpResourceOption<String> {
        private ServiceDomainOption() {
            super(Kind.SERVICE_DOMAIN, "serviceDomain");
        }
    }

    private static final class HttpServerHeaderOption extends HttpResourceOption<Boolean> {
        private HttpServerHeaderOption() {
            super(Kind.SERVER_HEADER, "serverHeaderEnabled", Boolean.TRUE);
        }
    }
 
    private static final class MaxAuthenticationAttemptsOption extends HttpResourceOption<Integer> {
        private MaxAuthenticationAttemptsOption() {
            super(Kind.MAX_AUTHENTICATION_ATTEMPTS, "max.authentication.attempts", 0);
        }
    }

    private static final class HttpRealmsOption extends HttpResourceOption<HttpRealmInfo[]> {
        private HttpRealmsOption() {
            super(Kind.REALMS, "realms", new HttpRealmInfo[0]);
        }
    }

}
