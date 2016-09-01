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
package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import java.nio.charset.Charset;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.USERNAME;

/**
 * STUN USERNAME attribute as described in https://tools.ietf.org/html/rfc5389#section-15.3.
 *
 */

public class Username extends Attribute {

    private String username;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public void setUsername(String username) {
        if (username.length() > 512) {
            throw new InvalidAttributeException("Username MUST be at most 512 characters");
        }
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public Username(byte[] value) {
        username = new String(value, UTF8);
    }

    @Override
    public short getType() {
        return USERNAME.getType();
    }

    @Override
    public short getLength() {
        return (short) (username.getBytes(UTF8).length);
    }

    @Override
    public byte[] getVariable() {
        return username.getBytes(UTF8);
    }
}
