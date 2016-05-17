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
package org.kaazing.gateway.server;

import java.util.ServiceLoader;

import org.kaazing.gateway.server.impl.GatewayCreator;

/**
 * The GatewayFactory is used to create instances of Gateway.  There is a single createGateway() method
 * to accomplish this.
 * @see Gateway
 */
public final class GatewayFactory {
    private static final ServiceLoader<GatewayCreator> loader = ServiceLoader.load(GatewayCreator.class);

    private GatewayFactory() {
    }

    /**
     * Creates an implementation of an Gateway.
     * @return a Gateway
     */
    public static Gateway createGateway() {
        Gateway gateway = null;
        for (GatewayCreator factory : loader) {
            gateway = factory.createGateway(gateway);
            factory.configureGateway(gateway);
        }

        if (gateway == null) {
            throw new RuntimeException("Failed to load GatewayCreator implementation class.");
        }

        return gateway;
    }
}
