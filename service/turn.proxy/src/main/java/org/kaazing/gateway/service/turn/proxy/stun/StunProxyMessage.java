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
package org.kaazing.gateway.service.turn.proxy.stun;

import java.util.List;

import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;

/**
 * Stun Message as defined in https://tools.ietf.org/html/rfc5389#section-6.
 *
 */
public class StunProxyMessage {

    private final StunMessageClass messageClass;
    private final StunMessageMethod method;
    private final byte[] transactionId;
    private final List<Attribute> attributes;
    public static final int MAGIC_COOKIE = 0x2112A442;
    private static final int PADDED_TO = 4;
    private boolean modified = false;

    public static final int HEADER_BYTES = 20;

    public StunProxyMessage(StunMessageClass messageClass, StunMessageMethod method, byte[] transactionId,
            List<Attribute> attributes) {
        this.messageClass = messageClass;
        this.method = method;
        this.transactionId = transactionId;
        this.attributes = attributes;
    }

    public StunMessageClass getMessageClass() {
        return messageClass;
    }

    public StunMessageMethod getMethod() {
        return method;
    }

    public short getMessageLength() {
        short length = 0;
        for (Attribute attribute : attributes) {
            length += 4;
            length += attributePaddedLength(attribute.getLength());
        }
        return length;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return String.format("%s %s with length: %d", messageClass.toString(), method.toString(), getMessageLength());
    }

    public static int attributePaddedLength(int length) {
        return ((length + PADDED_TO - 1) / PADDED_TO) * PADDED_TO;
    }

    /**
     * Has the proxy modified the message.
     * @return True if the message has been modified by the proxy service by calling the setter.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Set whether the proxy has modified the message;
     * @param modified Can also set value false to unmark the message as modified.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
