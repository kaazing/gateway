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
package org.kaazing.gateway.server.messaging;

import java.util.Map;

/**
 * Callback for all Session level events.
 * <p/>
 * <p/>
 * TODO: may want to add release failed TODO: may want to add acknowledge failed
 */
public interface MessagingSessionListener {

    /**
     * @param session
     */
    void acquired(MessagingSession session);

    /**
     * @param inUse   indicates if this session was already in use when reacquired
     * @param session
     */
    void reacquired(boolean inUse, MessagingSession session);

    /**
     * @param forced  indicates if the release was the result of a subsequent acquire
     * @param session
     */
    void released(boolean forced, MessagingSession session);

    /**
     * @param reason
     * @param message
     * @param sessionId
     * @param attributes
     * @param session
     */
    void acquireFailed(int reason, String message, String sessionId, Map<String, Object> attributes);

    /**
     * @param reason
     * @param message
     * @param destiantion
     * @param attributes
     * @param payload
     * @param session
     */
    void sendFailed(int reason, String message, String destiantion, Map<String, Object> attributes, Object payload,
                    MessagingSession session);
}
