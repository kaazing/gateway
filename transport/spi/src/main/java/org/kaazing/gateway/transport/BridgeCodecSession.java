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
package org.kaazing.gateway.transport;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import java.nio.ByteBuffer;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageBuffer;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;

public class BridgeCodecSession extends ProtocolCodecSessionEx implements BridgeSession {

    private static final MessageBufferAllocator MESSAGE_BUFFER_ALLOCATOR = new MessageBufferAllocator();

    private final IoBufferAllocatorEx<?> allocator;
    private final CachingMessageEncoder encoder;

    private Direction direction;
    private IoSessionEx parent;

    public BridgeCodecSession() {
        this(IO_MESSAGE_ENCODER, MESSAGE_BUFFER_ALLOCATOR);
    }

    public BridgeCodecSession(String cacheKey) {
        this(new CachingMessageEncoderImpl(cacheKey), MESSAGE_BUFFER_ALLOCATOR);
    }

    private BridgeCodecSession(CachingMessageEncoder encoder, IoBufferAllocatorEx<?> allocator) {
        this.encoder = encoder;
        this.allocator = allocator;
        this.direction = Direction.BOTH;
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        return encoder;
    }

    @Override
    public IoBufferAllocatorEx<?> getBufferAllocator() {
        return allocator;
    }

	public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    public void setParent(IoSessionEx parent) {
        this.parent = parent;
    }

    @Override
    public IoSessionEx getParent() {
        return parent;
    }

    @Override
    public ResourceAddress getLocalAddress() {
        return null;
    }

    @Override
    public ResourceAddress getRemoteAddress() {
        return null;
    }

    private static final class CachingMessageEncoderImpl extends CachingMessageEncoder {
        private final String cacheKey;

        private CachingMessageEncoderImpl(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode(cacheKey, encoder, message, allocator, flags);
        }
    }

    private static class MessageBufferAllocator extends AbstractIoBufferAllocatorEx<MessageBuffer<Message>> {

        @Override
        public ByteBuffer allocate(int capacity, int flags) {
            return allocateNioBuffer0(capacity, flags);
        }

        @Override
        public MessageBuffer<Message> wrap(ByteBuffer nioBuffer, int flags) {
            boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
            return shared ? new SharedMessageBuffer(nioBuffer) : new UnsharedMessageBuffer(nioBuffer);
        }
    }

    private static final class SharedMessageBuffer extends MessageBuffer<Message> {

        public SharedMessageBuffer(MessageBuffer<Message> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        public SharedMessageBuffer(ByteBuffer buf) {
            super(buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected SharedMessageBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected UnsharedMessageBuffer asUnsharedBuffer0() {
            return new UnsharedMessageBuffer(buf());
        }

        @Override
        protected MessageBuffer<Message> create0(MessageBuffer<Message> parent, ByteBuffer buf) {
            return new SharedMessageBuffer(parent, buf);
        }

    }

    private static final class UnsharedMessageBuffer extends MessageBuffer<Message> {

        public UnsharedMessageBuffer(MessageBuffer<Message> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        public UnsharedMessageBuffer(ByteBuffer buf) {
            super(buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected SharedMessageBuffer asSharedBuffer0() {
            return new SharedMessageBuffer(buf());
        }

        @Override
        protected UnsharedMessageBuffer asUnsharedBuffer0() {
            return this;
        }

        @Override
        protected MessageBuffer<Message> create0(MessageBuffer<Message> parent, ByteBuffer buf) {
            return new UnsharedMessageBuffer(parent, buf);
        }

    }
}
