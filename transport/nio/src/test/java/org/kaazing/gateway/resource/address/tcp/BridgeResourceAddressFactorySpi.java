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
package org.kaazing.gateway.resource.address.tcp;

import java.net.URI;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;

public class BridgeResourceAddressFactorySpi extends ResourceAddressFactorySpi {

    TcpResourceAddressFactorySpi delegate = new TcpResourceAddressFactorySpi();

    private URI transportedURI;

    public URI getTransportedURI() {
        return transportedURI;
    }

    public void setTransportedURI(URI transportedURI) {
        this.transportedURI = transportedURI;
    }


    @Override
    public String getSchemeName() {
        return "bridge";
    }

    @Override
    protected ResourceAddress newResourceAddress0(String original, String location) {
        URI uriLocation = URI.create(location);

        return new ResourceAddress(this, original, uriLocation) {
            @Override
            protected URI getTransportedURI() {
                return BridgeResourceAddressFactorySpi.this.getTransportedURI();
            }
        };
    }

    @Override
    protected String getTransportName() {
        return delegate.getTransportName();
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return delegate.getTransportFactory();
    }

    @Override
    protected String getProtocolName() {
        return delegate.getProtocolName();
    }

}
