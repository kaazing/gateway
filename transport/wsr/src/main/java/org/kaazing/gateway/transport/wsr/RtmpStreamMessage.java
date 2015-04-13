/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr;

public abstract class RtmpStreamMessage extends RtmpMessage {

    public enum StreamKind {
        SET_CHUNK_SIZE(1), ABORT(2), ACKNOWLEDGMENT(3), USER(4), WINDOW_ACKNOWLEDGMENT_SIZE(5), SET_PEER_BANDWIDTH(6), DATA_AMF3(15), COMMAND_AMF3(17), DATA_AMF0(18), COMMAND_AMF0(20);
        
        private final byte code;
        
        StreamKind(int code) {
            this.code = (byte)code;
        }
        
        public byte getCode() {
            return code;
        }
        
        public static StreamKind decode(int code) {
            switch (code) {
            case 1:
                return SET_CHUNK_SIZE;
            case 2:
                return ABORT;
            case 3:
                return ACKNOWLEDGMENT;
            case 4:
                return USER;
            case 5:
                return WINDOW_ACKNOWLEDGMENT_SIZE;
            case 6:
                return SET_PEER_BANDWIDTH;
            case 15:
            	return DATA_AMF3;
            case 17:
            	return COMMAND_AMF3;
            case 18:
                return DATA_AMF0;
            case 20:
                return COMMAND_AMF0;
            case 8:
            	// unexpected audio
            default:
                throw new IllegalArgumentException("Unexcepted code: " + code);
            }
        }
    }

    private int timestamp;
    private int messageStreamId;
    private int chunkStreamId;

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setMessageStreamId(int messageStreamId) {
        this.messageStreamId = messageStreamId;
    }

    public int getMessageStreamId() {
        return messageStreamId;
    }
    
    public final Kind getKind() {
        return Kind.STREAM;
    }
    
    public abstract StreamKind getStreamKind();

    public void setChunkStreamId(int chunkStreamId) {
        this.chunkStreamId = chunkStreamId;
    }

    public int getChunkStreamId() {
        return chunkStreamId;
    }
    
}
