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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.kaazing.gateway.service.messaging.MessagingMessage;

public class DefaultMessagingMessage implements MessagingMessage, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private Object payload;
    private Map<String, Object> attributes;

    public DefaultMessagingMessage(MessagingMessage toCopy) {
        this.id = toCopy.getId();
        this.payload = toCopy.getPayload();
        Map<String, Object> attributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : toCopy.attributes()) {
            attributes.put(entry.getKey(), entry.getValue());
        }
        this.attributes = attributes;
    }

    public DefaultMessagingMessage() {
        this.attributes = new HashMap<>();
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public Iterable<Map.Entry<String, Object>> attributes() {
        return attributes.entrySet();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return attributes.put(name, value);
    }

}
