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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.ERROR_CODE;

/**
 * STUN ERROR-CODE attribute as described in https://tools.ietf.org/html/rfc5389#section-15.6.
 *
 */

public class ErrorCode extends Attribute {

    private int errorCode;
    private String errMsg;

    public void setErrMsg(String errMsg) {
        if (errMsg.length() > 128) {
            throw new InvalidAttributeException("Error message MUST be at most 128 characters");
        }
        this.errMsg = errMsg;
    }

    public void setErrorCode(int errorCode) {
        if (errorCode < 300 || errorCode > 699) {
            throw new InvalidAttributeException("Error code MUST be in the range of 300 to 699");
        }
        this.errorCode = errorCode;
    }

    @Override
    public short getType() {
        return ERROR_CODE.getType();
    }

    @Override
    public short getLength() {
        return (short) (4 + errMsg.getBytes(Charset.forName("UTF-8")).length);
    }

    @Override
    public byte[] getVariable() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getLength());
        byteBuffer.put((byte) 0x00);
        byteBuffer.putShort((short) (errorCode / 100));
        byteBuffer.put((byte) (errorCode % 100));
        byteBuffer.put(errMsg.getBytes(Charset.forName("UTF-8")));
        return byteBuffer.array();
    }
}
