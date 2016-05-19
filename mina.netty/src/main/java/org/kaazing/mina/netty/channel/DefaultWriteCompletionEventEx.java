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
package org.kaazing.mina.netty.channel;

import static org.jboss.netty.channel.Channels.succeededFuture;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.WriteCompletionEvent;

/**
 * The default {@link WriteCompletionEvent} implementation.
 */
public class DefaultWriteCompletionEventEx implements WriteCompletionEvent {

    private Channel channel;
    private long writtenAmount;

    /**
     * Initializes the instance.
     */
    public void init(Channel channel, long writtenAmount) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (writtenAmount <= 0) {
            throw new IllegalArgumentException(
                    "writtenAmount must be a positive integer: " + writtenAmount);
        }

        this.channel = channel;
        this.writtenAmount = writtenAmount;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ChannelFuture getFuture() {
        return succeededFuture(getChannel());
    }

    @Override
    public long getWrittenAmount() {
        return writtenAmount;
    }

    @Override
    public String toString() {
        String channelString = getChannel().toString();
        StringBuilder buf = new StringBuilder(channelString.length() + 32);
        buf.append(channelString);
        buf.append(" WRITTEN_AMOUNT: ");
        buf.append(getWrittenAmount());
        return buf.toString();
    }
}
