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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

public class BridgeConnectHandler extends AbstractBridgeHandler {

    public static final AttributeKey DELEGATE_KEY = new AttributeKey(BridgeConnectHandler.class, "delegate");

    @Override
    protected IoHandler getHandler(IoSession session, boolean throwIfNull) throws Exception {
        IoHandler handler = (IoHandler)session.getAttribute(DELEGATE_KEY);
        if (handler == null && throwIfNull) {
            throw new Exception("No handler found for: " + session.getLocalAddress());
        }
        return handler;
    }
}
