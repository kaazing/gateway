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

public abstract class AmqpConnectionMessage extends AmqpClassMessage {
    /*
    public static final int CONNECTION_START     = 10;
    public static final int CONNECTION_START_OK  = 11;
    public static final int CONNECTION_SECURE    = 20;
    public static final int CONNECTION_SECURE_OK = 21;
    public static final int CONNECTION_TUNE      = 30;
    public static final int CONNECTION_TUNE_OK   = 31;
    public static final int CONNECTION_OPEN      = 40;
    public static final int CONNECTION_OPEN_OK   = 41;
    public static final int CONNECTION_CLOSE     = 50;
    public static final int CONNECTION_CLOSE_OK  = 51;
    */

    public static final String AMQP_AUTHENTICATION_MECHANISM = "amqp_authentication_mechanism";
    
    public enum ConnectionMethodKind {
        START((short)10),
        START_OK((short)11),
        SECURE((short)20),
        SECURE_OK((short)21),
        TUNE((short)30),
        TUNE_OK((short)31),
        OPEN((short)40),
        OPEN_OK((short)41),
        CLOSE((short)50),
        CLOSE_OK((short)51);

        private final short methodIndex;
        
        ConnectionMethodKind(short methodId) {
            this.methodIndex = methodId;
        }
        
        public short methodId() {
            return this.methodIndex;
        }
        
        public static ConnectionMethodKind get(short methodId) {
            switch (methodId) {
                case 10:
                    return START;
                case 11:
                    return START_OK;
                case 20:
                    return SECURE;
                case 21:
                    return SECURE_OK;
                case 30:
                    return TUNE;
                case 31:
                    return TUNE_OK;
                case 40:
                    return OPEN;
                case 41:
                    return OPEN_OK;
                case 50:
                    return CLOSE;
                case 51:
                    return CLOSE_OK;
                default:
                    String s = "Protocol violation - Invalid method-id: " + methodId;
                    throw new IllegalStateException(s);
            }
        }
    }

    @Override
    public AmqpFrame getFrame() {
        return AmqpFrame.METHOD;
    }

    @Override
    public ClassKind getClassKind() {
        return ClassKind.CONNECTION;
    }
    
    public abstract ConnectionMethodKind getMethodKind();
}
