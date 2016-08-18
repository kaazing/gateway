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

public enum StunMessageClass {
    //@formatter:off
    REQUEST((short) 0x000),
    INDICATION((short) 0x010),
    RESPONSE((short) 0x100),
    ERROR((short) 0x110);
    //@formatter:on

    private static final short SIGNIFICANT_BITS = 0x110;
    private final short value;

    StunMessageClass(short value) {
        this.value = value;
    }

    public static StunMessageClass valueOf(short leadingBitesAndMessageType) {
        switch (leadingBitesAndMessageType & SIGNIFICANT_BITS) {
        case 0x000:
            return REQUEST;
        case 0x010:
            return INDICATION;
        case 0x100:
            return RESPONSE;
        case 0x110:
            return ERROR;
        default:
            throw new IllegalStateException(
                String.format("No such STUN class from: 0x%02X", leadingBitesAndMessageType)
            );
        }
    }

    public short getValue() {
        return value;
    }
}
