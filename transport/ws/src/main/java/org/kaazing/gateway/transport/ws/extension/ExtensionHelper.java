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
package org.kaazing.gateway.transport.ws.extension;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;

/**
 * This interface provides methods which can be used within extension filters.
 */
public interface ExtensionHelper {

    /**
     * Signal re-authentication on the WebSocket connection
     * @param session        transport session on whose filter chain the WebSocket frames flow
     * @param loginContext   the new authenticated login context
     */
    void setLoginContext(IoSession session, ResultAwareLoginContext loginContext);

    /**
     * Close the WebSocket connection associated with the given transport session.
     * Also cleans up any login context state that should be cleaned up.
     * @param session    transport session on whose filter chain the WebSocket frames flow
     */
    void closeWebSocketConnection(IoSession session);

}
