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
package org.kaazing.gateway.transport.http.resource.impl;

import static java.lang.String.format;
import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Set;

import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResourceFactorySpi;

public final class HttpCrossOriginJavaBridgeVerifierFactorySpi extends HttpDynamicResourceFactorySpi {

    private static final String CROSS_ORIGIN_BRIDGE_RESOURCE = "xjb";
    private static final Set<String> RESOURCE_NAMES = singleton(CROSS_ORIGIN_BRIDGE_RESOURCE);

    private final HttpCrossOriginJavaBridgeVerifier verifier;

    public HttpCrossOriginJavaBridgeVerifierFactorySpi() {
        verifier = new HttpCrossOriginJavaBridgeVerifier();
    }

    @Override
    public Collection<String> getResourceNames() {
        return RESOURCE_NAMES;
    }

    @Override
    public HttpDynamicResource newDynamicResource(String resourceName) {
        
        if (!CROSS_ORIGIN_BRIDGE_RESOURCE.equals(resourceName)) {
            throw new IllegalArgumentException(format("Unrecognized resource name: '%s'", resourceName));
        }
        
        return verifier;
    }

}
