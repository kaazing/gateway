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
package org.kaazing.gateway.server.messaging.buffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferEntry;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferListener;
import org.kaazing.gateway.util.GL;

public class MessageBufferListenerSupport implements MessageBufferListener {

    private final Map<MessageBufferListener, MessageBufferListener> listeners;

    public MessageBufferListenerSupport() {
        listeners = new ConcurrentHashMap<>();
    }

    public void addMessageBufferListener(MessageBufferListener listener) {
        listeners.put(listener, listener);
    }

    public void removeMessageBufferListener(MessageBufferListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void messageAdded(MessageBufferEntry newMessage) {
        for (MessageBufferListener listener : listeners.keySet()) {
            try {
                listener.messageAdded(newMessage);
            } catch (Exception e) {
                GL.warn("stompservice", "Unable to process message buffer listener:\n{}", e);
            }
        }
    }
}
