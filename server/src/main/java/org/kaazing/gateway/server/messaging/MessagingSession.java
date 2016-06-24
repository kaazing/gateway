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
import org.kaazing.gateway.service.messaging.AttributeStore;
import org.kaazing.gateway.service.messaging.MessagingMessage;

/**
 * Represents an active link between a client and the messaging provider.
 * <p/>
 * TODO: Transactions are not yet supported. TODO: May want to fire a messageSent event on the session for send acknowledgement.
 */
public interface MessagingSession extends AttributeStore {

    /**
     * Get the globally unique session identifier.
     *
     * @return unique session id
     */
    String getId();

    /**
     * Acquire a subscription for this session to the specified destination.
     * <p/>
     * Notes on how some specific JMS features would be implemented:
     * <p/>
     * selector - no selector, any filtering is done by protocol handlers or clients of messagingprovider ack_mode - no ack mode,
     * all messages must be acknoledged by the client no_local - resolved by session.getId() and msg.getSender() compare
     * is_durable - placed on the release for now. this will require us to pay for replication for all subscriptions. This could
     * be changed.
     *
     * @param destination
     * @param attributes
     * @param hints
     * @param listener
     */
    void acquireSubscription(String destination, Map<String, Object> attributes, MessagingHints hints,
                             MessagingSubscriptionListener listener);

    /**
     * @param destination
     * @param hints
     * @param listener
     */
    void acquireSubscription(String destination, MessagingHints hints, MessagingSubscriptionListener listener);

    /**
     * @param subscriptionId
     * @param hints
     * @param listener
     */
    void reacquireSubscription(String subscriptionId, MessagingHints hints, MessagingSubscriptionListener listener);

    /**
     * @param subscriptionId
     * @param reacquireable
     */
    void releaseSubscription(String subscriptionId, boolean reacquireable);

    /**
     * @param subscriptionId
     */
    void releaseSubscription(String subscriptionId);

    /**
     * @param subscription
     * @param reacquireable
     */
    void releaseSubscription(MessagingSubscription subscription, boolean reacquireable);

    /**
     * @param subscription
     */
    void releaseSubscription(MessagingSubscription subscription);

    /**
     * @param destination
     * @param attributes
     * @param payload
     */
    void send(String destination, Map<String, Object> attributes, Object payload);

    /**
     * @param destination
     * @param payload
     */
    void send(String destination, Object payload);

    /**
     * @param messageId
     */
    void acknowledgeMessage(String messageId);

    /**
     * @param message
     */
    void acknowledgeMessage(MessagingMessage message);

}
