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
package org.kaazing.gateway.service.amqp.amqp091.message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.amqp.amqp091.filter.AmqpFilter;


public class AmqpProtocolHeaderMessage extends AmqpMessage {
    public static final byte[] PROTOCOL_0_9_1_DEFAULT_HEADER = 
                                        new byte[] {65, 77, 81, 80, 0, 0, 9, 1};

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    
    private ByteBuffer    protocolHeader;
    
    public AmqpProtocolHeaderMessage() {
        protocolHeader = ByteBuffer.wrap(PROTOCOL_0_9_1_DEFAULT_HEADER);
    }

    @Override
    public MessageKind getMessageKind() {
        return MessageKind.PROTOCOL_HEADER;
    }
    
    @Override
    public <S extends IoSession> void messageReceived(AmqpFilter<S> filter,
            NextFilter nextFilter, S session) throws Exception {
        filter.messageReceived(nextFilter, session, this);
    }

    @Override
    public <S extends IoSession> void filterWrite(AmqpFilter<S> filter,
            NextFilter nextFilter, S session, WriteRequest writeRequest)
            throws Exception {
        filter.filterWrite(nextFilter, session, writeRequest, this);
    }

    @Override
    public <S extends IoSession> void messageSent(AmqpFilter<S> filter,
            NextFilter nextFilter, S session, WriteRequest writeRequest)
            throws Exception {
        filter.messageSent(nextFilter, session, writeRequest, this);
    }

    public byte[] getProtocolHeader() {
        return protocolHeader.array();
    }
    
    public void setProtocolHeader(ByteBuffer protocolHeader) {
        this.protocolHeader = protocolHeader;
    }

    public void setProtocolHeader(String protocolHeader) {
        this.protocolHeader = ByteBuffer.wrap(protocolHeader.getBytes(UTF_8));
    }
    
    public void setProtocolHeader(byte[] protocolHeader) {
        this.protocolHeader = ByteBuffer.wrap(protocolHeader);
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("PROTOCOL_HEADER: ");
        buffer.append(PROTOCOL_0_9_1_DEFAULT_HEADER);
        return buffer.toString();
    }

}
