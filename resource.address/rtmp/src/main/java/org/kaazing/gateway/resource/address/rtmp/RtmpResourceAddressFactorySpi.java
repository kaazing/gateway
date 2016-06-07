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
package org.kaazing.gateway.resource.address.rtmp;


import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceFactories.keepAuthorityOnly;
import static org.kaazing.gateway.resource.address.rtmp.RtmpResourceAddress.TRANSPORT_NAME;

import java.net.URI;

import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;

public class RtmpResourceAddressFactorySpi extends ResourceAddressFactorySpi<RtmpResourceAddress> {

    private static final String SCHEME_NAME = "rtmp";
    private static final int SCHEME_PORT = 1935;
    private static final ResourceFactory TRANSPORT_FACTORY = keepAuthorityOnly("tcp");

    private static final String PROTOCOL_NAME = "rtmp/1.0";

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
    
    @Override
    protected RtmpResourceAddress newResourceAddress0(String original, String location) {

        URI uriLocation = URI.create(location);
        String path = uriLocation.getPath();

        if (uriLocation.getHost() == null) {
            throw new IllegalArgumentException(format("Missing host in URI: %s", location));
        }

        if (uriLocation.getPort() == -1) {
            throw new IllegalArgumentException(format("Missing port in URI: %s", location));
        }

        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException(format("Missing path in URI: %s", location));
        }

        return new RtmpResourceAddress(this, original, uriLocation);
    }

}
