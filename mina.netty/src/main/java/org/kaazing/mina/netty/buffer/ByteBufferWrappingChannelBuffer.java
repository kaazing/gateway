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
package org.kaazing.mina.netty.buffer;

import static org.kaazing.mina.netty.buffer.ByteBufferWrappingChannelBufferFactory.OPTIMIZE_PERFORMANCE_CLIENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

import org.jboss.netty.buffer.AbstractChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;

/**
 * A NIO {@link ByteBuffer} based buffer.
 */
public final class ByteBufferWrappingChannelBuffer extends AbstractChannelBuffer {

    private ByteBuffer buffer;
    private ByteOrder order;
    private int capacity;

    /**
     * Creates a new buffer which wraps the specified buffer's slice.
     */
    public ChannelBuffer wrap(ByteBuffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        int position = buffer.position();
        int limit = buffer.limit();

        this.order = buffer.order();
        this.buffer = buffer;
        this.capacity = buffer.capacity();
        setIndex(position, limit);
        return this;
    }

    private ChannelBuffer wrap(ByteBufferWrappingChannelBuffer buffer) {
        this.buffer = buffer.buffer;
        order = buffer.order;
        capacity = buffer.capacity;
        setIndex(buffer.readerIndex(), buffer.writerIndex());
        return this;
    }

    @Override
    public int readableBytes() {
        return buffer.remaining();
    }

    @Override
    public ChannelBufferFactory factory() {
        if (buffer.isDirect()) {
            return DirectChannelBufferFactory.getInstance(order());
        } else {
            return HeapChannelBufferFactory.getInstance(order());
        }
    }

    @Override
    public boolean isDirect() {
        return buffer.isDirect();
    }

    @Override
    public ByteOrder order() {
        return order;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    @Override
    public byte getByte(int index) {
        return buffer.get(index);
    }

    @Override
    public short getShort(int index) {
        return buffer.getShort(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return  (getByte(index)     & 0xff) << 16 |
                (getByte(index + 1) & 0xff) <<  8 |
                getByte(index + 2) & 0xff;
    }

    @Override
    public int getInt(int index) {
        return buffer.getInt(index);
    }

    @Override
    public long getLong(int index) {
        return buffer.getLong(index);
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        if (dst instanceof ByteBufferWrappingChannelBuffer) {
            ByteBufferWrappingChannelBuffer bbdst = (ByteBufferWrappingChannelBuffer) dst;
            // note: single-threaded, no need to duplicate
            ByteBuffer data = bbdst.buffer;

            // but *do* need to reset position and limit afterwards
            int position = data.position();
            int limit = data.limit();

            data.limit(dstIndex + length).position(dstIndex);
            getBytes(index, data);

            // reset position and limit
            data.limit(limit).position(position);
        } else if (buffer.hasArray()) {
            dst.setBytes(dstIndex, buffer.array(), index + buffer.arrayOffset(), length);
        } else {
            dst.setBytes(dstIndex, this, index, length);
        }
    }

    @Override
    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        // note: single-threaded, no need to duplicate
        ByteBuffer data = buffer;

        // but *do* need to reset position and limit afterwards
        int position = data.position();
        int limit = data.limit();

        try {
            data.limit(index + length).position(index);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need "
                    + (index + length) + ", maximum is " + data.limit());
        }
        data.get(dst, dstIndex, length);

        // reset position and limit
        data.limit(limit).position(position);
    }

    @Override
    public void getBytes(int index, ByteBuffer dst) {
        // note: single-threaded, no need to duplicate
        ByteBuffer data = buffer;
        int bytesToCopy = Math.min(capacity() - index, dst.remaining());

        // but *do* need to reset position and limit afterwards
        int position = data.position();
        int limit = data.limit();

        try {
            data.limit(index + bytesToCopy).position(index);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need "
                    + (index + bytesToCopy) + ", maximum is " + data.limit());
        }
        dst.put(data);

        // reset position and limit
        data.limit(limit).position(position);
    }

    @Override
    public void setByte(int index, int value) {
        buffer.put(index, (byte) value);
    }

    @Override
    public void setShort(int index, int value) {
        buffer.putShort(index, (short) value);
    }

    @Override
    public void setMedium(int index, int   value) {
        setByte(index,     (byte) (value >>> 16));
        setByte(index + 1, (byte) (value >>>  8));
        setByte(index + 2, (byte) value);
    }

    @Override
    public void setInt(int index, int   value) {
        buffer.putInt(index, value);
    }

    @Override
    public void setLong(int index, long  value) {
        buffer.putLong(index, value);
    }

    @Override
    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        if (src instanceof ByteBufferWrappingChannelBuffer) {
            ByteBufferWrappingChannelBuffer bbsrc = (ByteBufferWrappingChannelBuffer) src;
            ByteBuffer data = bbsrc.buffer.duplicate();

            data.limit(srcIndex + length).position(srcIndex);
            setBytes(index, data);
        } else if (buffer.hasArray()) {
            src.getBytes(srcIndex, buffer.array(), index + buffer.arrayOffset(), length);
        } else {
            src.getBytes(srcIndex, this, index, length);
        }
    }

    @Override
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        ByteBuffer data = buffer.duplicate();
        data.limit(index + length).position(index);
        data.put(src, srcIndex, length);
    }

    @Override
    public void setBytes(int index, ByteBuffer src) {
        if (index == 0) {
            wrap(src);
        }
        else if (buffer == null) {
            ByteBuffer data = src.duplicate();
            data.limit(index + src.remaining()).position(index);
            data.put(src);
        }
        else {
            ByteBuffer data = buffer.duplicate();
            data.limit(index + src.remaining()).position(index);
            data.put(src);
        }
    }

    @Override
    public void getBytes(int index, OutputStream out, int length) throws IOException {
        if (length == 0) {
            return;
        }

        if (buffer.hasArray()) {
            out.write(
                    buffer.array(),
                    index + buffer.arrayOffset(),
                    length);
        } else {
            byte[] tmp = new byte[length];
            ((ByteBuffer) buffer.duplicate().position(index)).get(tmp);
            out.write(tmp);
        }
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        if (length == 0) {
            return 0;
        }

        return out.write((ByteBuffer) buffer.duplicate().position(index).limit(index + length));
    }

    @Override
    public int setBytes(int index, InputStream in, int length)
            throws IOException {

        int readBytes = 0;

        if (buffer.hasArray()) {
            index += buffer.arrayOffset();
            do {
                int localReadBytes = in.read(buffer.array(), index, length);
                if (localReadBytes < 0) {
                    if (readBytes == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                readBytes += localReadBytes;
                index += localReadBytes;
                length -= localReadBytes;
            } while (length > 0);
        } else {
            byte[] tmp = new byte[length];
            int i = 0;
            do {
                int localReadBytes = in.read(tmp, i, tmp.length - i);
                if (localReadBytes < 0) {
                    if (readBytes == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                readBytes += localReadBytes;
                i += readBytes;
            } while (i < tmp.length);
            ((ByteBuffer) buffer.duplicate().position(index)).put(tmp);
        }

        return readBytes;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length)
            throws IOException {

        ByteBuffer slice = (ByteBuffer) buffer.duplicate().limit(index + length).position(index);
        int readBytes = 0;

        while (readBytes < length) {
            int localReadBytes;
            try {
                localReadBytes = in.read(slice);
            } catch (ClosedChannelException e) {
                localReadBytes = -1;
            }
            if (localReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                } else {
                    return readBytes;
                }
            }
            if (localReadBytes == 0) {
                break;
            }
            readBytes += localReadBytes;
        }

        return readBytes;
    }

    @Override
    public ByteBuffer toByteBuffer(int index, int length) {
        if (index == 0 && length == capacity()) {
            // no need to duplicate
            return buffer.order(order());
        } else {
            if (OPTIMIZE_PERFORMANCE_CLIENT) {
                // no need to duplicate or slice
                return ((ByteBuffer) buffer.position(
                        index).limit(index + length)).order(order());
            }
            else {
                return ((ByteBuffer) buffer.duplicate().position(
                        index).limit(index + length)).slice().order(order());
            }
        }
    }

    @Override
    public ChannelBuffer slice(int index, int length) {
        if (index == 0 && length == capacity()) {
            ChannelBuffer slice = duplicate();
            slice.setIndex(0, length);
            return slice;
        } else {
            if (index >= 0 && length == 0) {
                return ChannelBuffers.EMPTY_BUFFER;
            }
            return new ByteBufferWrappingChannelBuffer().wrap(
                    ((ByteBuffer) buffer.duplicate().position(
                            index).limit(index + length)).order(order()));
        }
    }

    @Override
    public ChannelBuffer duplicate() {
        return new ByteBufferWrappingChannelBuffer().wrap(this);
    }

    @Override
    public ChannelBuffer copy(int index, int length) {
        ByteBuffer src;
        try {
            src = (ByteBuffer) buffer.duplicate().position(index).limit(index + length);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need "
                    + (index + length));
        }

        ByteBuffer dst = buffer.isDirect() ? ByteBuffer.allocateDirect(length) : ByteBuffer.allocate(length);
        dst.put(src);
        dst.order(order());
        dst.clear();
        return new ByteBufferWrappingChannelBuffer().wrap(dst);
    }
}
