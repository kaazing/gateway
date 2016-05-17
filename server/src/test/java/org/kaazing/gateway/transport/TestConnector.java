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
package org.kaazing.gateway.transport;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;

/**
 * Used for testing resource injection (see GatewayContextResolverTest)
 */
public final class TestConnector implements BridgeConnector {

    @Override
    public void dispose() {

    }

    @Override
    public ConnectFuture connect(ResourceAddress address,
                                 IoHandler handler,
                                 IoSessionInitializer<? extends ConnectFuture> initializer) {
        return null;
    }

    @Override
    public void connectInit(ResourceAddress address) {

    }

    @Override
    public void connectDestroy(ResourceAddress address) {

    }

}
