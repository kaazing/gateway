/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
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
package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.CompositeChannelBuffer;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.internal.ByteBufferUtil;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.WritableByteChannel;

final class SocketSendBufferPool implements ExternalResourceReleasable {

    private static final SendBuffer EMPTY_BUFFER = new EmptySendBuffer();

    private static final int DEFAULT_PREALLOCATION_SIZE = 65536;
    private static final int ALIGN_SHIFT = 4;
    private static final int ALIGN_MASK = 15;

    private PreallocationRef poolHead;
    private Preallocation current = new Preallocation(DEFAULT_PREALLOCATION_SIZE);

    // sharing works because this is single-threaded from NIO Worker thread
    private SharedUnpooledSendBuffer sharedUnpooled = new SharedUnpooledSendBuffer();
    private SharedPooledSendBuffer sharedPooled = new SharedPooledSendBuffer();

    SendBuffer acquire(Object message) {
        if (message instanceof ChannelBuffer) {
            return acquire((ChannelBuffer) message);
        }
        if (message instanceof FileRegion) {
            return acquire((FileRegion) message);
        }

        throw new IllegalArgumentException(
                "unsupported message type: " + message.getClass());
    }

    private SendBuffer acquire(FileRegion src) {
        if (src.getCount() == 0) {
            return EMPTY_BUFFER;
        }
        return new FileSendBuffer(src);
    }

    private SendBuffer acquire(ChannelBuffer src) {
        final int size = src.readableBytes();
        if (size == 0) {
            return EMPTY_BUFFER;
        }

        if (src instanceof CompositeChannelBuffer && ((CompositeChannelBuffer) src).useGathering()) {
            return new GatheringSendBuffer(src.toByteBuffers());
        }

        if (src.isDirect()) {
            SharedUnpooledSendBuffer sharedUnpooled = getSharedUnpooled();
            return sharedUnpooled.init(src.toByteBuffer());
        }
        if (src.readableBytes() > DEFAULT_PREALLOCATION_SIZE) {
            SharedUnpooledSendBuffer sharedUnpooled = getSharedUnpooled();
            return sharedUnpooled.init(src.toByteBuffer());
        }

        Preallocation current = this.current;
        ByteBuffer buffer = current.buffer;
        int remaining = buffer.remaining();
        SharedPooledSendBuffer dst;

        if (size < remaining) {
            int nextPos = buffer.position() + size;
            ByteBuffer slice = buffer.duplicate();
            buffer.position(align(nextPos));
            slice.limit(nextPos);
            current.refCnt ++;
            SharedPooledSendBuffer sharedPooled = getSharedPooled();
            dst = sharedPooled.init(current, slice);
        } else if (size > remaining) {
            this.current = current = getPreallocation();
            buffer = current.buffer;
            ByteBuffer slice = buffer.duplicate();
            buffer.position(align(size));
            slice.limit(size);
            current.refCnt ++;
            SharedPooledSendBuffer sharedPooled = getSharedPooled();
            dst = sharedPooled.init(current, slice);
        } else { // size == remaining
            current.refCnt ++;
            this.current = getPreallocation0();
            SharedPooledSendBuffer sharedPooled = getSharedPooled();
            dst = sharedPooled.init(current, current.buffer);
        }

        ByteBuffer dstbuf = dst.buffer;
        dstbuf.mark();
        src.getBytes(src.readerIndex(), dstbuf);
        dstbuf.reset();
        return dst;
    }

    private SharedUnpooledSendBuffer getSharedUnpooled() {
        if (!sharedUnpooled.canInitialize()) {
            sharedUnpooled = new SharedUnpooledSendBuffer();
        }
        assert sharedUnpooled.canInitialize();
        return sharedUnpooled;
    }

    private SharedPooledSendBuffer getSharedPooled() {
        if (!sharedPooled.canInitialize()) {
            sharedPooled = new SharedPooledSendBuffer();
        }
        assert sharedPooled.canInitialize();
        return sharedPooled;
    }

    private Preallocation getPreallocation() {
        Preallocation current = this.current;
        if (current.refCnt == 0) {
            current.buffer.clear();
            return current;
        }

        return getPreallocation0();
    }

    private Preallocation getPreallocation0() {
        PreallocationRef ref = poolHead;
        if (ref != null) {
            do {
                Preallocation p = ref.get();
                ref = ref.next;

                if (p != null) {
                    poolHead = ref;
                    return p;
                }
            } while (ref != null);

            poolHead = ref;
        }

        return new Preallocation(DEFAULT_PREALLOCATION_SIZE);
    }

    private static int align(int pos) {
        int q = pos >>> ALIGN_SHIFT;
        int r = pos & ALIGN_MASK;
        if (r != 0) {
            q ++;
        }
        return q << ALIGN_SHIFT;
    }

    private static final class Preallocation {
        final ByteBuffer buffer;
        int refCnt;

        Preallocation(int capacity) {
            buffer = ByteBuffer.allocateDirect(capacity);
        }
    }

    private final class PreallocationRef extends SoftReference<Preallocation> {
        final PreallocationRef next;

        PreallocationRef(Preallocation prealloation, PreallocationRef next) {
            super(prealloation);
            this.next = next;
        }
    }

    interface SendBuffer {
        boolean finished();
        long writtenBytes();
        long totalBytes();

        long transferTo(WritableByteChannel ch) throws IOException;
        long transferTo(DatagramChannel ch, SocketAddress raddr) throws IOException;

        void release();
    }

    static class UnpooledSendBuffer implements SendBuffer {

        final ByteBuffer buffer;
        final int initialPos;

        UnpooledSendBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
            initialPos = buffer.position();
        }

        public final boolean finished() {
            return !buffer.hasRemaining();
        }

        public final long writtenBytes() {
            return buffer.position() - initialPos;
        }

        public final long totalBytes() {
            return buffer.limit() - initialPos;
        }

        public final long transferTo(WritableByteChannel ch) throws IOException {
            return ch.write(buffer);
        }

        public final long transferTo(DatagramChannel ch, SocketAddress raddr) throws IOException {
            return ch.send(buffer, raddr);
        }

        public void release() {
            // Unpooled.
        }
    }

    static class SharedUnpooledSendBuffer implements SendBuffer {

        ByteBuffer buffer;
        int initialPos;

        public final SendBuffer init(ByteBuffer buffer) {
            this.buffer = buffer;
            initialPos = buffer.position();
            return this;
        }

        public final boolean canInitialize() {
            return buffer == null || finished();
        }

        public final boolean finished() {
            return !buffer.hasRemaining();
        }

        public final long writtenBytes() {
            return buffer.position() - initialPos;
        }

        public final long totalBytes() {
            return buffer.limit() - initialPos;
        }

        public final long transferTo(WritableByteChannel ch) throws IOException {
            return ch.write(buffer);
        }

        public final long transferTo(DatagramChannel ch, SocketAddress raddr) throws IOException {
            return ch.send(buffer, raddr);
        }

        public void release() {
            // Unpooled.
        }
    }

    final class PooledSendBuffer extends UnpooledSendBuffer {

        private final Preallocation parent;

        PooledSendBuffer(Preallocation parent, ByteBuffer buffer) {
            super(buffer);
            this.parent = parent;
        }

        @Override
        public void release() {
            final Preallocation parent = this.parent;
            if (-- parent.refCnt == 0) {
                parent.buffer.clear();
                if (parent != current) {
                    poolHead = new PreallocationRef(parent, poolHead);
                }
            }
        }
    }

    final class SharedPooledSendBuffer extends SharedUnpooledSendBuffer {

        private Preallocation parent;

        public SharedPooledSendBuffer init(Preallocation parent, ByteBuffer buffer) {
            init(buffer);
            this.parent = parent;
            return this;
        }

        @Override
        public void release() {
            final Preallocation parent = this.parent;
            if (-- parent.refCnt == 0) {
                parent.buffer.clear();
                if (parent != current) {
                    poolHead = new PreallocationRef(parent, poolHead);
                }
            }
        }
    }

    static class GatheringSendBuffer implements SendBuffer {

        private final ByteBuffer[] buffers;
        private final int last;
        private long written;
        private final int total;

        GatheringSendBuffer(ByteBuffer[] buffers) {
            this.buffers = buffers;
            last = buffers.length - 1;
            int total = 0;
            for (ByteBuffer buf: buffers) {
                total += buf.remaining();
            }
            this.total = total;
        }

        public boolean finished() {
            return !buffers[last].hasRemaining();
        }

        public long writtenBytes() {
            return written;
        }

        public long totalBytes() {
            return total;
        }

        public long transferTo(WritableByteChannel ch) throws IOException {
            if (ch instanceof GatheringByteChannel) {
                 long w = ((GatheringByteChannel) ch).write(buffers);
                 written += w;
                 return w;
            } else {
                int send = 0;
                for (ByteBuffer buf: buffers) {
                    if (buf.hasRemaining()) {
                        int w = ch.write(buf);
                        if (w == 0) {
                            break;
                        } else {
                            send += w;
                        }
                    }
                }
                written += send;
                return send;
            }
        }

        public long transferTo(DatagramChannel ch, SocketAddress raddr) throws IOException {
            int send = 0;
            for (ByteBuffer buf: buffers) {
                if (buf.hasRemaining()) {
                    int w = ch.send(buf, raddr);
                    if (w == 0) {
                        break;
                    } else {
                        send += w;
                    }
                }
            }
            written += send;

            return send;
        }

        public void release() {
            // nothing todo
        }
    }

    final class FileSendBuffer implements SendBuffer {

        private final FileRegion file;
        private long writtenBytes;

        FileSendBuffer(FileRegion file) {
            this.file = file;
        }

        public boolean finished() {
            return writtenBytes >= file.getCount();
        }

        public long writtenBytes() {
            return writtenBytes;
        }

        public long totalBytes() {
            return file.getCount();
        }

        public long transferTo(WritableByteChannel ch) throws IOException {
            long localWrittenBytes = file.transferTo(ch, writtenBytes);
            writtenBytes += localWrittenBytes;
            return localWrittenBytes;
        }

        public long transferTo(DatagramChannel ch, SocketAddress raddr) {
            throw new UnsupportedOperationException();
        }

        public void release() {
            if (file instanceof DefaultFileRegion) {
                if (((DefaultFileRegion) file).releaseAfterTransfer()) {
                    // Make sure the FileRegion resource are released otherwise it may cause a FD
                    // leak or something similar
                    file.releaseExternalResources();
                }
            }
        }
    }

    static final class EmptySendBuffer implements SendBuffer {

        public boolean finished() {
            return true;
        }

        public long writtenBytes() {
            return 0;
        }

        public long totalBytes() {
            return 0;
        }

        public long transferTo(WritableByteChannel ch) {
            return 0;
        }

        public long transferTo(DatagramChannel ch, SocketAddress raddr) {
            return 0;
        }

        public void release() {
            // Unpooled.
        }
    }

    public void releaseExternalResources() {
        if (current.buffer != null) {
            ByteBufferUtil.destroy(current.buffer);
        }
    }
}
