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

import java.util.zip.CRC32;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.FINGERPRINT;

public class Fingerprint extends Attribute {

    protected byte[] value = new byte[4];

    private static final long DEFAULT_XOR_VALUE = 0x5354554e;

    public Fingerprint() {
        // nothing to do, the calculation is done in a separate method
    }

    public Fingerprint(byte[] key) {
        this.value = key;
    }

    public void calculate(byte[] input) {
        CRC32 crc32 = new CRC32();
        crc32.update(input);
        long xorKey = crc32.getValue() ^ DEFAULT_XOR_VALUE;
        for (int i = 3; i >= 0; i--) {
            value[i] = (byte)(xorKey & 0xFF);
            xorKey >>= 8;
        }
    }

    @Override
    public short getType() {
        return FINGERPRINT.getType();
    }

    @Override
    public short getLength() {
        return (short) value.length;
    }

    @Override
    public byte[] getVariable() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : getVariable()) {
            sb.append(String.format("%02X ", b));
        }
        return String.format("%s - %s", super.toString(), sb.toString());
    }
}
