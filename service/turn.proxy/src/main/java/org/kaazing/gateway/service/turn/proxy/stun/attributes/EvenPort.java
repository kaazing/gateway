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

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.EVEN_PORT;

/**
 * TURN Even Port attribute as described in https://tools.ietf.org/html/rfc5766#section-14.6.
 *
 */
public class EvenPort extends Attribute {

    private boolean reserveNextHigherPort;

    public EvenPort(byte[] value) {
        reserveNextHigherPort = value[0] == ((byte) 0x80);
    }

    @Override
    public short getType() {
        return EVEN_PORT.getType();
    }

    @Override
    public short getLength() {
        return 4;
    }

    @Override
    public byte[] getVariable() {
        return reserveNextHigherPort ? new byte[]{(byte) 0x80, 0x00, 0x00, 0x00} : new byte[]{0x00, 0x00, 0x00, 0x00};
    }

    public void setReserveNextHigherPort(boolean flag) {
        reserveNextHigherPort = flag;
    }

    public boolean getReserveNextHigherPort() {
        return reserveNextHigherPort;
    }
}
