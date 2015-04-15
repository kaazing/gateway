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

import static org.kaazing.gateway.transport.wsr.RtmpStreamMessage.StreamKind.SET_PEER_BANDWIDTH;


public class RtmpSetPeerBandwidthMessage extends RtmpStreamMessage {

    public enum LimitType {
        HARD, SOFT, DYNAMIC
    }
    private long windowSize;
    private LimitType limitType;

    public String toString() {
        return String.format("%s messageStreamId=%s windowSize=%s", getStreamKind(), getMessageStreamId(),
                getWindowSize());
    }

    @Override
    public StreamKind getStreamKind() {
        return SET_PEER_BANDWIDTH;
    }

    public void setWindowSize(long windowSize) {
        this.windowSize = windowSize;
    }

    public long getWindowSize() {
        return windowSize;
    }

    public void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }

    public LimitType getLimitType() {
        return limitType;
    }

}
