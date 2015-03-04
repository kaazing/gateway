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

import java.nio.ByteBuffer;

public abstract class RtmpHandshakeMessage extends RtmpMessage {

    public static final int NONCE_LENGTH = 1528;
    private int timestamp1;
    private int timestamp2;
    private ByteBuffer nonce;

    public RtmpHandshakeMessage(int t1, int t2, ByteBuffer nonce) {
        timestamp1 = t1;
        timestamp2 = t2;
        this.nonce = nonce;
    }

    public RtmpHandshakeMessage() {
        timestamp1 = (0);
        timestamp2 = 0;
        nonce = ByteBuffer.allocate(NONCE_LENGTH);
    }

    @Override
    public abstract Kind getKind();

    public int getTimestamp1() {
        return timestamp1;
    }

    public int getTimestamp2() {
        return timestamp2;
    }

    public void setTimestamp2(int t) {
        timestamp2 = t;
    }
    
    public ByteBuffer getNonce() {
        return nonce;
    }

}
