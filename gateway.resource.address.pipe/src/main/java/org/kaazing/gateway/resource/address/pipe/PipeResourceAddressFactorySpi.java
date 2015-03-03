/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.resource.address.pipe;

import static org.kaazing.gateway.resource.address.pipe.PipeResourceAddress.TRANSPORT_NAME;

import java.net.URI;

import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;

public class PipeResourceAddressFactorySpi extends ResourceAddressFactorySpi<PipeResourceAddress> {
    
    private static final String SCHEME_NAME = "pipe";

    private static final String PROTOCOL_NAME = "pipe";

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    protected String getTransportName() {
        return TRANSPORT_NAME;
    }
    
    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }
    
    @Override
    protected ResourceFactory getTransportFactory() {
        return null;
    }

    @Override
    protected PipeResourceAddress newResourceAddress0(URI original, URI location) {

        // Unlike a normal-looking URI, our custom "pipe://" does not have
        // host/port/path components.  Instead, the authority component
        // suffices.

        String pipeName = location.getAuthority();
        if (pipeName == null) {
            throw new IllegalArgumentException(String.format("URI %s missing pipe name", location));
        }

        return new PipeResourceAddress(original, location);
    }
    
}
