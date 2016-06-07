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
package org.kaazing.gateway.service.messaging.buffer;

import java.io.Serializable;

import org.kaazing.gateway.service.messaging.MessagingMessage;

public class MessageBufferEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int id;
    private final MessagingMessage message;
    private final long expires;

    public MessageBufferEntry(int id, MessagingMessage message) {
        this.id = id;
        this.expires = 0;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public long getExpires() {
        return expires;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[id=")
          .append(id)
          .append(",expires=")
          .append(expires)
          .append(",message=")
          .append(message)
          .append(']');
        return sb.toString();
    }

    public MessagingMessage getMessage() {
        return message;
    }
}
