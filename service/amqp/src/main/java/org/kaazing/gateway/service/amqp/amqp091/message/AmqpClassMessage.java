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

import org.kaazing.gateway.service.amqp.amqp091.AmqpFrame;

public abstract class AmqpClassMessage extends AmqpMessage {
    public static final short FRAME_END = 206; // 0xce

    private int channelId;    // This is short as per the spec.
    
    public enum ClassKind {
        CONNECTION((short)10),
        CHANNEL((short)20),
        EXCHANGE((short)40),
        QUEUE((short)50),
        BASIC((short)60),
        TRANSACTION((short)90);
        
        private final short classIndex;
        
        ClassKind(short classId) {
            this.classIndex = classId;
        }
        
        public short classId() {
            return this.classIndex;
        }
        
        public static ClassKind get(short classId) {
            switch (classId) {
                case 10:
                    return CONNECTION;
                case 20:
                    return CHANNEL;
                case 40:
                    return EXCHANGE;
                case 50:
                    return QUEUE;
                case 60:
                    return BASIC;
                case 90:
                    return TRANSACTION;
                default:
                    String s = "Protocol violation - Invalid class-id: " + classId;
                    throw new IllegalStateException(s);
            }
        }
    }

    @Override
    public MessageKind getMessageKind() {
        return MessageKind.CLASS;
    }
    
    public int getChannelId() {
        return channelId;
    }
    
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    
    public abstract AmqpFrame getFrame();
        
    public abstract ClassKind getClassKind();
    
}
