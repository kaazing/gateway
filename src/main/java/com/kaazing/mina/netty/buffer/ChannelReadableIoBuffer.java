/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.buffer;

import static io.netty.buffer.Unpooled.unmodifiableBuffer;
import static io.netty.util.CharsetUtil.UTF_8;
import io.netty.buffer.ByteBuf;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;

public class ChannelReadableIoBuffer extends IoBuffer {

	private final ByteBuf byteBuf;
	private int markedReaderIndex = -1;
	
	public ChannelReadableIoBuffer(ByteBuf byteBuf) {
		this.byteBuf = byteBuf;
	}
	
	public ByteBuf byteBuf() {
		return byteBuf;
	}
	
	@Override
	public int compareTo(IoBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            byte v1 = this.get(i);
            byte v2 = that.get(j);
            if (v1 == v2) {
                continue;
            }
            if (v1 < v2) {
                return -1;
            }

            return +1;
        }
        return this.remaining() - that.remaining();
	}

	@Override
	public void free() {
		byteBuf.unsafe().release();
	}

	@Override
	public ByteBuffer buf() {
		return byteBuf.nioBuffer();
	}

	@Override
	public boolean isDirect() {
		return byteBuf.isDirect();
	}

	@Override
	public boolean isDerived() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return byteBuf.nioBuffer().isReadOnly();
	}

	@Override
	public int minimumCapacity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer minimumCapacity(int minimumCapacity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int capacity() {
		return byteBuf.capacity();
	}

	@Override
	public IoBuffer capacity(int newCapacity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAutoExpand() {
		return false;
	}

	@Override
	public IoBuffer setAutoExpand(boolean autoExpand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAutoShrink() {
		return false;
	}

	@Override
	public IoBuffer setAutoShrink(boolean autoShrink) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer expand(int expectedRemaining) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer expand(int position, int expectedRemaining) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer shrink() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int position() {
		return byteBuf.readerIndex();
	}

	@Override
	public IoBuffer position(int newPosition) {
		byteBuf.readerIndex(newPosition);
		return this;
	}

	@Override
	public int limit() {
		return byteBuf.writerIndex();
	}

	@Override
	public IoBuffer limit(int newLimit) {
		byteBuf.writerIndex(newLimit);
		return this;
	}

	@Override
	public IoBuffer mark() {
		markedReaderIndex = byteBuf.readerIndex();
		byteBuf.markReaderIndex();
		return this;
	}

	@Override
	public int markValue() {
		return markedReaderIndex;
	}

	@Override
	public IoBuffer reset() {
		byteBuf.resetReaderIndex();
		return this;
	}

	@Override
	public IoBuffer clear() {
		byteBuf.clear();
		markedReaderIndex = -1;
		return null;
	}

	@Override
	public IoBuffer sweep() {
        clear();
        return fillAndReset(remaining());
	}

	@Override
	public IoBuffer sweep(byte value) {
        clear();
        return fillAndReset(value, remaining());
	}

	@Override
	public IoBuffer flip() {
		// no-op for reader
		return this;
	}

	@Override
	public IoBuffer rewind() {
		byteBuf.readerIndex(0);
        markedReaderIndex = -1;
		return this;
	}

	@Override
	public int remaining() {
		return byteBuf.readableBytes();
	}

	@Override
	public boolean hasRemaining() {
		return byteBuf.readable();
	}

	@Override
	public IoBuffer duplicate() {
		return new ChannelReadableIoBuffer(byteBuf.duplicate());
	}

	@Override
	public IoBuffer slice() {
		return new ChannelReadableIoBuffer(byteBuf.slice());
	}

	@Override
	public IoBuffer asReadOnlyBuffer() {
		return new ChannelReadableIoBuffer(unmodifiableBuffer(byteBuf));
	}

	@Override
	public boolean hasArray() {
		return byteBuf.hasArray();
	}

	@Override
	public byte[] array() {
		return byteBuf.array();
	}

	@Override
	public int arrayOffset() {
		return byteBuf.arrayOffset();
	}

	@Override
	public byte get() {
		return byteBuf.readByte();
	}

	@Override
	public short getUnsigned() {
		return byteBuf.readUnsignedByte();
	}

	@Override
	public IoBuffer put(byte b) {
		byteBuf.writeByte(b);
		return this;
	}

	@Override
	public byte get(int index) {
		return byteBuf.getByte(index);
	}

	@Override
	public short getUnsigned(int index) {
		return byteBuf.getUnsignedByte(index);
	}

	@Override
	public IoBuffer put(int index, byte b) {
		byteBuf.setByte(index, b);
		return this;
	}

	@Override
	public IoBuffer get(byte[] dst, int offset, int length) {
		byteBuf.readBytes(dst, offset, length);
		return this;
	}

	@Override
	public IoBuffer get(byte[] dst) {
		byteBuf.getBytes(byteBuf.readerIndex(), dst);
		return this;
	}

	@Override
	public IoBuffer getSlice(int index, int length) {
		return new ChannelReadableIoBuffer(byteBuf.slice(index, length));
	}

	@Override
	public IoBuffer getSlice(int length) {
		return new ChannelReadableIoBuffer(byteBuf.readSlice(length));
	}

	@Override
	public IoBuffer put(ByteBuffer src) {
		byteBuf.writeBytes(src);
		return this;
	}

	@Override
	public IoBuffer put(IoBuffer src) {
		byteBuf.writeBytes(src.buf());
		return this;
	}

	@Override
	public IoBuffer put(byte[] src, int offset, int length) {
		byteBuf.writeBytes(src, offset, length);
		return this;
	}

	@Override
	public IoBuffer put(byte[] src) {
		byteBuf.writeBytes(src);
		return this;
	}

	@Override
	public IoBuffer compact() {
		byteBuf.discardReadBytes();
		return this;
	}

	@Override
	public ByteOrder order() {
		return byteBuf.order();
	}

	@Override
	public IoBuffer order(ByteOrder bo) {
		byteBuf.order(bo);
		return this;
	}

	@Override
	public char getChar() {
		return byteBuf.readChar();
	}

	@Override
	public IoBuffer putChar(char value) {
		byteBuf.writeChar(value);
		return this;
	}

	@Override
	public char getChar(int index) {
		return byteBuf.getChar(index);
	}

	@Override
	public IoBuffer putChar(int index, char value) {
		byteBuf.setChar(index, value);
		return this;
	}

	@Override
	public CharBuffer asCharBuffer() {
		return byteBuf.nioBuffer().asCharBuffer();
	}

	@Override
	public short getShort() {
		return byteBuf.readShort();
	}

	@Override
	public int getUnsignedShort() {
		return byteBuf.readUnsignedShort();
	}

	@Override
	public IoBuffer putShort(short value) {
		byteBuf.writeShort(value);
		return this;
	}

	@Override
	public short getShort(int index) {
		return byteBuf.getShort(index);
	}

	@Override
	public int getUnsignedShort(int index) {
		return byteBuf.getUnsignedShort(index);
	}

	@Override
	public IoBuffer putShort(int index, short value) {
		byteBuf.setShort(index, value);
		return this;
	}

	@Override
	public ShortBuffer asShortBuffer() {
		return byteBuf.nioBuffer().asShortBuffer();
	}

	@Override
	public int getInt() {
		return byteBuf.readInt();
	}

	@Override
	public long getUnsignedInt() {
		return byteBuf.readUnsignedInt();
	}

	@Override
	public int getMediumInt() {
		return byteBuf.readMedium();
	}

	@Override
	public int getUnsignedMediumInt() {
		return byteBuf.readUnsignedMedium();
	}

	@Override
	public int getMediumInt(int index) {
		return byteBuf.getMedium(index);
	}

	@Override
	public int getUnsignedMediumInt(int index) {
		return byteBuf.getUnsignedMedium(index);
	}

	@Override
	public IoBuffer putMediumInt(int value) {
		byteBuf.writeMedium(value);
		return this;
	}

	@Override
	public IoBuffer putMediumInt(int index, int value) {
		byteBuf.setMedium(index, value);
		return this;
	}

	@Override
	public IoBuffer putInt(int value) {
		byteBuf.writeInt(value);
		return this;
	}

	@Override
	public int getInt(int index) {
		return byteBuf.getInt(index);
	}

	@Override
	public long getUnsignedInt(int index) {
		return byteBuf.getUnsignedInt(index);
	}

	@Override
	public IoBuffer putInt(int index, int value) {
		byteBuf.setInt(index, value);
		return this;
	}

	@Override
	public IntBuffer asIntBuffer() {
		return byteBuf.nioBuffer().asIntBuffer();
	}

	@Override
	public long getLong() {
		return byteBuf.readLong();
	}

	@Override
	public IoBuffer putLong(long value) {
		byteBuf.writeLong(value);
		return this;
	}

	@Override
	public long getLong(int index) {
		return byteBuf.getLong(index);
	}

	@Override
	public IoBuffer putLong(int index, long value) {
		byteBuf.setLong(index, value);
		return this;
	}

	@Override
	public LongBuffer asLongBuffer() {
		return byteBuf.nioBuffer().asLongBuffer();
	}

	@Override
	public float getFloat() {
		return byteBuf.readFloat();
	}

	@Override
	public IoBuffer putFloat(float value) {
		byteBuf.writeFloat(value);
		return this;
	}

	@Override
	public float getFloat(int index) {
		return byteBuf.getFloat(index);
	}

	@Override
	public IoBuffer putFloat(int index, float value) {
		byteBuf.setFloat(index, value);
		return this;
	}

	@Override
	public FloatBuffer asFloatBuffer() {
		return byteBuf.nioBuffer().asFloatBuffer();
	}

	@Override
	public double getDouble() {
		return byteBuf.readDouble();
	}

	@Override
	public IoBuffer putDouble(double value) {
		byteBuf.writeDouble(value);
		return this;
	}

	@Override
	public double getDouble(int index) {
		return byteBuf.getDouble(index);
	}

	@Override
	public IoBuffer putDouble(int index, double value) {
		byteBuf.setDouble(index, value);
		return this;
	}

	@Override
	public DoubleBuffer asDoubleBuffer() {
		return byteBuf.nioBuffer().asDoubleBuffer();
	}

	@Override
	public InputStream asInputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream asOutputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getHexDump() {
		return byteBuf.toString(UTF_8);
	}

	@Override
	public String getHexDump(int lengthLimit) {
		return byteBuf.toString(0, lengthLimit, UTF_8);
	}

	@Override
	public String getString(CharsetDecoder decoder)
			throws CharacterCodingException {
		int readerIndex = byteBuf.readerIndex();
		int writerIndex = byteBuf.writerIndex();
		int nulAt = byteBuf.indexOf(readerIndex, writerIndex, (byte)0x00);
		if (nulAt == -1) {
			nulAt = writerIndex;
		}
		return byteBuf.toString(readerIndex, nulAt - readerIndex, decoder.charset());
	}

	@Override
	public String getString(int fieldSize, CharsetDecoder decoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putString(CharSequence val, CharsetEncoder encoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putString(CharSequence val, int fieldSize,
			CharsetEncoder encoder) throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPrefixedString(CharsetDecoder decoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPrefixedString(int prefixLength, CharsetDecoder decoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
			CharsetEncoder encoder) throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
			int padding, CharsetEncoder encoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putPrefixedString(CharSequence val, int prefixLength,
			int padding, byte padValue, CharsetEncoder encoder)
			throws CharacterCodingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getObject() throws ClassNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getObject(ClassLoader classLoader)
			throws ClassNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putObject(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean prefixedDataAvailable(int prefixLength) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean prefixedDataAvailable(int prefixLength, int maxDataLength) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(byte b) {
		return byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.writerIndex(), b);
	}

	@Override
	public IoBuffer skip(int size) {
		byteBuf.skipBytes(size);
		return this;
	}

	@Override
	public IoBuffer fill(byte value, int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer fillAndReset(byte value, int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer fill(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer fillAndReset(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnum(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnum(int index, Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnumShort(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnumInt(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnum(Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnum(int index, Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnumShort(Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnumShort(int index, Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnumInt(Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IoBuffer putEnumInt(int index, Enum<?> e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSet(int index,
			Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index,
			Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index,
			Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index,
			Class<E> enumClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSet(Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSet(int index, Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetShort(Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetShort(int index, Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetInt(Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetInt(int index, Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetLong(Set<E> set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Enum<E>> IoBuffer putEnumSetLong(int index, Set<E> set) {
		throw new UnsupportedOperationException();
	}

}
