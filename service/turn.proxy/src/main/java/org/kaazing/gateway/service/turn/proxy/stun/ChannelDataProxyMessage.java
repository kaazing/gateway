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

public class ChannelDataProxyMessage {

    private short channelNumber;
    private short messageLength;
    private byte[] appData;
    
    public static final int HEADER_BYTES = 4;

    public short getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(short channelNumber) {
        this.channelNumber = channelNumber;
    }

    public short getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(short messageLength) {
        this.messageLength = messageLength;
    }

    public byte[] getAppData() {
        return appData;
    }

    public void setAppData(byte[] appData) {
        this.appData = appData;
    }

    public ChannelDataProxyMessage(short channelNumber, short messageLength, byte[] appData) {
        super();
        this.channelNumber = channelNumber;
        this.messageLength = messageLength;
        this.appData = appData;
    }
    

}
