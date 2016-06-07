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

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.amqp.amqp091.filter.AmqpFilter;

public class AmqpTuneMessage extends AmqpConnectionMessage {
    private int   maxChannels;
    private int   maxFrameSize;
    private int   heartbeatDelay;
    
    public AmqpTuneMessage() {
        maxChannels = 256;
        maxFrameSize = 65535; // 0xFFFF
        heartbeatDelay = 0;
    }

    @Override
    public ConnectionMethodKind getMethodKind() {
        return ConnectionMethodKind.TUNE;
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

    public int getHeartbeatDelay() {
        return heartbeatDelay;
    }
    
    public int getMaxChannels() {
        return maxChannels;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setHeartbeatDelay(int heartbeatDelay) {
        this.heartbeatDelay = heartbeatDelay;
    }
    
    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
    }
    
    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("TUNE: ");
        buffer.append("max-channels = '").append(maxChannels).append("'")
              .append("   max-frame-size = '").append(maxFrameSize).append("'")
              .append("   heartbeat-delay = '").append(heartbeatDelay).append("'");

        return buffer.toString();
    }
}
