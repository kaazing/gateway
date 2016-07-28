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

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.FINGERPRINT;

import org.kaazing.gateway.service.turn.proxy.stun.StunMessage;

public class Fingerprint extends Attribute {

    protected byte[] value;

    private static final byte[] DEFAULT_XOR_VALUE = new byte[]{0x53, 0x54, 0x55, 0x4e};

    public Fingerprint(byte[] key) {
        this.value = key;
        // TODO: decoding of the key
    }

    @Override
    public short getType() {
        return FINGERPRINT.getType();
    }

    @Override
    public short getLength() {
        return 16;
    }

    @Override
    public byte[] getVariable() {
        return value;
    }

    public void computeVariable(StunMessage message) {
        // TODO update the variable
    }

}
