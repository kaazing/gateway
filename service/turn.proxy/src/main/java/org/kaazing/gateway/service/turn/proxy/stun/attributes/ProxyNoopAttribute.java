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

/**
 * When we pass Attribute through proxy without modifying or needing to understand it
 *
 */
public class ProxyNoopAttribute extends Attribute {

    private final short type;
    private final short length;
    private final byte[] value;

    public ProxyNoopAttribute(short type, short length, byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public short getLength() {
        return length;
    }

    @Override
    public byte[] getVariable() {
        return value;
    }

}
