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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.mina.core.future.UnbindFuture;

/**
 * Used for testing resource injection (see GatewayContextResolverTest)
 */
public final class TestAcceptor implements BridgeAcceptor {
    public Collection<?> extensions = Collections.singletonList(new TestTransportExtension());

    @Override
    public void dispose() {

    }

    @Override
    public void bind(ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<? extends IoFuture> initializer) {

    }

    @Override
    public UnbindFuture unbind(ResourceAddress address) {
        return null;
    }

    @Override
    public IoHandler getHandler(ResourceAddress address) {
        return null;
    }

}
