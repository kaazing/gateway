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

import org.kaazing.gateway.resource.address.Protocol;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;

/**
 * IoSession initializer for bridge sessions that allows per-protocol parent initializers to be registered.
 * 
 * @param <T>
 *            the future
 */
@Deprecated // incompatible with repeated protocol layering, and only used by balancer, which will move to an IoFilter<HttpSession> instead
public interface BridgeSessionInitializer<T extends IoFuture> extends IoSessionInitializer<T> {
    /**
     * Gets the parent initializer for a bridge session.
     * 
     * @param protocol
     *            the protocol of the parent session
     * @return the parent initializer
     */
    BridgeSessionInitializer<T> getParentInitializer(Protocol protocol);
}
