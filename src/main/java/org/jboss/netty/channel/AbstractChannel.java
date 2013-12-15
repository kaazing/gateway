/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 * Copied From: Netty-3.6.3-Final
 */

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel;

import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

/**
 * A skeletal {@link Channel} implementation.
 */
public abstract class AbstractChannel implements Channel {

    static final ConcurrentMap<Integer, Channel> allChannels = new ConcurrentHashMap<Integer, Channel>();

    private static final Random random = new Random();

    private static Integer allocateId(Channel channel) {
        Integer id = random.nextInt();
        for (;;) {
            // Loop until a unique ID is acquired.
            // It should be found in one loop practically.
            if (allChannels.putIfAbsent(id, channel) == null) {
                // Successfully acquired.
                return id;
            } else {
                // Taken by other channel at almost the same moment.
                id = id.intValue() + 1;
            }
        }
    }

    private final Integer id;
    private final Channel parent;
    private final ChannelFactory factory;
    private final ChannelPipeline pipeline;
    private final ChannelFuture succeededFuture = new SucceededChannelFuture(this);
    private final ChannelCloseFuture closeFuture = new ChannelCloseFuture();
    private volatile int interestOps = OP_READ;

    /** Cache for the string representation of this channel */
    private boolean strValConnected;
    private String strVal;
    private volatile Object attachment;

    /**
     * Creates a new instance.
     *
     * @param parent
     *        the parent of this channel. {@code null} if there's no parent.
     * @param factory
     *        the factory which created this channel
     * @param pipeline
     *        the pipeline which is going to be attached to this channel
     * @param sink
     *        the sink which will receive downstream events from the pipeline
     *        and send upstream events to the pipeline
     */
    protected AbstractChannel(
            Channel parent, ChannelFactory factory,
            ChannelPipeline pipeline, ChannelSink sink) {

        this.parent = parent;
        this.factory = factory;
        this.pipeline = pipeline;

        id = allocateId(this);

        pipeline.attach(this, sink);
    }

    /**
     * (Internal use only) Creates a new temporary instance with the specified
     * ID.
     *
     * @param parent
     *        the parent of this channel. {@code null} if there's no parent.
     * @param factory
     *        the factory which created this channel
     * @param pipeline
     *        the pipeline which is going to be attached to this channel
     * @param sink
     *        the sink which will receive downstream events from the pipeline
     *        and send upstream events to the pipeline
     */
    protected AbstractChannel(
            Integer id,
            Channel parent, ChannelFactory factory,
            ChannelPipeline pipeline, ChannelSink sink) {

        this.id = id;
        this.parent = parent;
        this.factory = factory;
        this.pipeline = pipeline;
        pipeline.attach(this, sink);
    }

    public final Integer getId() {
        return id;
    }

    public Channel getParent() {
        return parent;
    }

    public ChannelFactory getFactory() {
        return factory;
    }

    public ChannelPipeline getPipeline() {
        return pipeline;
    }

    /**
     * Returns the cached {@link SucceededChannelFuture} instance.
     */
    protected ChannelFuture getSucceededFuture() {
        return succeededFuture;
    }

    /**
     * Returns the {@link FailedChannelFuture} whose cause is an
     * {@link UnsupportedOperationException}.
     */
    protected ChannelFuture getUnsupportedOperationFuture() {
        return new FailedChannelFuture(this, new UnsupportedOperationException());
    }

    /**
     * Returns the ID of this channel.
     */
    @Override
    public final int hashCode() {
        return id;
    }

    /**
     * Returns {@code true} if and only if the specified object is identical
     * with this channel (i.e: {@code this == o}).
     */
    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    /**
     * Compares the {@linkplain #getId() ID} of the two channels.
     */
    public final int compareTo(Channel o) {
        return getId().compareTo(o.getId());
    }

    public boolean isOpen() {
        return !closeFuture.isDone();
    }

    /**
     * Marks this channel as closed.  This method is intended to be called by
     * an internal component - please do not call it unless you know what you
     * are doing.
     *
     * @return {@code true} if and only if this channel was not marked as
     *                      closed yet
     */
    protected boolean setClosed() {
        // Deallocate the current channel's ID from allChannels so that other
        // new channels can use it.
        allChannels.remove(id);

        return closeFuture.setClosed();
    }

    public ChannelFuture bind(SocketAddress localAddress) {
        return Channels.bind(this, localAddress);
    }

    public ChannelFuture unbind() {
        return Channels.unbind(this);
    }

    public ChannelFuture close() {
        ChannelFuture returnedCloseFuture = Channels.close(this);
        assert closeFuture == returnedCloseFuture;
        return closeFuture;
    }

    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }

    public ChannelFuture connect(SocketAddress remoteAddress) {
        return Channels.connect(this, remoteAddress);
    }

    public ChannelFuture disconnect() {
        return Channels.disconnect(this);
    }

    public int getInterestOps() {
        return interestOps;
    }

    public ChannelFuture setInterestOps(int interestOps) {
        System.out.println(String.format("[%s] setting interestOps on channel [%d] to %d",
                System.currentTimeMillis(), getId(), interestOps));
        return Channels.setInterestOps(this, interestOps);
    }

    /**
     * Sets the {@link #getInterestOps() interestOps} property of this channel
     * immediately.  This method is intended to be called by an internal
     * component - please do not call it unless you know what you are doing.
     */
    protected void setInterestOpsNow(int interestOps) {
        this.interestOps = interestOps;
    }

    public boolean isReadable() {
        return (getInterestOps() & OP_READ) != 0;
    }

    public boolean isWritable() {
        return (getInterestOps() & OP_WRITE) == 0;
    }

    public ChannelFuture setReadable(boolean readable) {
        if (readable) {
            return setInterestOps(getInterestOps() | OP_READ);
        } else {
            return setInterestOps(getInterestOps() & ~OP_READ);
        }
    }

    public ChannelFuture write(Object message) {
        return Channels.write(this, message);
    }

    public ChannelFuture write(Object message, SocketAddress remoteAddress) {
        return Channels.write(this, message, remoteAddress);
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }
    /**
     * Returns the {@link String} representation of this channel.  The returned
     * string contains the {@linkplain #getId() ID}, {@linkplain #getLocalAddress() local address},
     * and {@linkplain #getRemoteAddress() remote address} of this channel for
     * easier identification.
     */
    @Override
    public String toString() {
        boolean connected = isConnected();
        if (strValConnected == connected && strVal != null) {
            return strVal;
        }

        StringBuilder buf = new StringBuilder(128);
        buf.append("[id: 0x");
        buf.append(getIdString());

        SocketAddress localAddress = getLocalAddress();
        SocketAddress remoteAddress = getRemoteAddress();
        if (remoteAddress != null) {
            buf.append(", ");
            if (getParent() == null) {
                buf.append(localAddress);
                buf.append(connected? " => " : " :> ");
                buf.append(remoteAddress);
            } else {
                buf.append(remoteAddress);
                buf.append(connected? " => " : " :> ");
                buf.append(localAddress);
            }
        } else if (localAddress != null) {
            buf.append(", ");
            buf.append(localAddress);
        }

        buf.append(']');

        String strVal = buf.toString();
        this.strVal = strVal;
        strValConnected = connected;
        return strVal;
    }

    private String getIdString() {
        String answer = Integer.toHexString(id.intValue());
        switch (answer.length()) {
        case 0:
            answer = "00000000";
            break;
        case 1:
            answer = "0000000" + answer;
            break;
        case 2:
            answer = "000000" + answer;
            break;
        case 3:
            answer = "00000" + answer;
            break;
        case 4:
            answer = "0000" + answer;
            break;
        case 5:
            answer = "000" + answer;
            break;
        case 6:
            answer = "00" + answer;
            break;
        case 7:
            answer = '0' + answer;
            break;
        }
        return answer;
    }

    private final class ChannelCloseFuture extends DefaultChannelFuture {

        public ChannelCloseFuture() {
            super(AbstractChannel.this, false);
        }

        @Override
        public boolean setSuccess() {
            // User is not supposed to call this method - ignore silently.
            return false;
        }

        @Override
        public boolean setFailure(Throwable cause) {
            // User is not supposed to call this method - ignore silently.
            return false;
        }

        boolean setClosed() {
            return super.setSuccess();
        }
    }
}
