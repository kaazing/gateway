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

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.MAPPED_ADDRESS;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Stun Mapped Address attribute as defined in https://tools.ietf.org/html/rfc5389#section-15.1
 */
public class MappedAddress extends AbstractAddress {

    public MappedAddress(InetSocketAddress address) {
        super(address);
    }

    public MappedAddress(byte[] variable) {
        super(variable);
    }

    @Override
    public short getType() {
        return MAPPED_ADDRESS.getType();
    }

    @Override
    public byte[] getVariable() {
        ByteBuffer byteBuffer = ByteBuffer.allocate((getFamily() == Family.IPV4) ? 4 + 32/8 : 4 + 128/8);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put(getFamily().getEncoding());
        byteBuffer.putShort((short) getPort());
        byteBuffer.put(getAddress());
        return byteBuffer.array();
    }

}
