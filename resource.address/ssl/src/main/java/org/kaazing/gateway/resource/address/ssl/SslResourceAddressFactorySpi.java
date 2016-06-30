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
package org.kaazing.gateway.resource.address.ssl;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceFactories.keepAuthorityOnly;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.ENCRYPTION_ENABLED;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.KEY_SELECTOR;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.NEED_CLIENT_AUTH;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.PROTOCOLS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.TRANSPORT_NAME;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.WANT_CLIENT_AUTH;

import java.net.URI;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.security.KeySelector;
import org.kaazing.gateway.util.ssl.SslCipherSuites;

public class SslResourceAddressFactorySpi extends ResourceAddressFactorySpi<SslResourceAddress> {

    private static final String SCHEME_NAME = "ssl";
    
    private static final ResourceFactory TRANSPORT_FACTORY = keepAuthorityOnly("tcp");
    
    private static final String PROTOCOL_NAME = "ssl";

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
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
    
    @Override
    protected void parseNamedOptions0(String location, ResourceOptions options,
                                      Map<String, Object> optionsByName) {
        Object ciphers = optionsByName.remove(CIPHERS.name());
        if (ciphers instanceof String) {
            options.setOption(CIPHERS, SslCipherSuites.resolveCSV((String) ciphers));
        } else if (ciphers instanceof String[]) {
            options.setOption(CIPHERS, (String[]) ciphers);
        } else {
            assert ciphers == null;
        }

        Boolean encryption = (Boolean) optionsByName.remove(ENCRYPTION_ENABLED.name());
        if (encryption != null) {
            options.setOption(ENCRYPTION_ENABLED, encryption);
        }

        Boolean wantClientAuth = (Boolean) optionsByName.remove(WANT_CLIENT_AUTH.name());
        if (wantClientAuth != null) {
            options.setOption(WANT_CLIENT_AUTH, wantClientAuth);
        }

        Boolean needClientAuth = (Boolean) optionsByName.remove(NEED_CLIENT_AUTH.name());
        if (needClientAuth != null) {
            options.setOption(NEED_CLIENT_AUTH, needClientAuth);
        }

        KeySelector keySelector = (KeySelector) optionsByName.remove(KEY_SELECTOR.name());
        if (keySelector != null) {
            options.setOption(KEY_SELECTOR, keySelector);
        }

        String[] protocols = (String[]) optionsByName.remove(PROTOCOLS.name());
        if ( protocols != null ) {
            options.setOption(PROTOCOLS, protocols);
        }

    }
    
    @Override
    protected SslResourceAddress newResourceAddress0(String original, String location) {

        URI uriLocation = URI.create(location);
        String path = uriLocation.getPath();

        if (uriLocation.getHost() == null) {
            throw new IllegalArgumentException(format("Missing host in URI: %s", location));
        }

        if (uriLocation.getPort() == -1) {
            throw new IllegalArgumentException(format("Missing port in URI: %s", location));
        }

        if (path != null && !path.isEmpty()) {
            throw new IllegalArgumentException(format("Unexpected path \"%s\" in URI: %s", path, location));
        }

        return new SslResourceAddress(this, original, uriLocation);
    }

    @Override
    protected void setOptions(SslResourceAddress address, ResourceOptions options, Object qualifier) {

        super.setOptions(address, options, qualifier);
        
        address.setOption0(CIPHERS, options.getOption(CIPHERS));
        address.setOption0(ENCRYPTION_ENABLED, options.getOption(ENCRYPTION_ENABLED));
        address.setOption0(WANT_CLIENT_AUTH, options.getOption(WANT_CLIENT_AUTH));
        address.setOption0(NEED_CLIENT_AUTH, options.getOption(NEED_CLIENT_AUTH));
        address.setOption0(KEY_SELECTOR, options.getOption(KEY_SELECTOR));
        address.setOption0(PROTOCOLS, options.getOption(PROTOCOLS));
    }

}
