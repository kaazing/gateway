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

import org.kaazing.gateway.service.messaging.MessagingMessage;


/**
 * Callback for subscription level events.
 * <p/>
 * TODO: may want to add release failed
 */
public interface MessagingSubscriptionListener {

    /**
     * @param subscription
     * @param session
     */
    void acquired(MessagingSubscription subscription, MessagingSession session);

    /**
     * @param subscription
     * @param session
     */
    void reacquired(MessagingSubscription subscription, MessagingSession session);

    /**
     * @param forced
     * @param subscription
     * @param session
     */
    void released(boolean forced, MessagingSubscription subscription, MessagingSession session);

    /**
     * @param reason
     * @param message
     * @param destination
     * @param session
     */
    void acquireFailed(int reason, String message, String destination, MessagingSession session);

    /**
     * @param message
     * @param subscription
     * @param session
     */
    void receiveMessage(MessagingMessage message, MessagingSubscription subscription, MessagingSession session);

}
