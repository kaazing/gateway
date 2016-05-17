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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
/* Differences from IoBuffer class in Mina 2.0.0-RC1 include:
 * 1. Use instance level flags instead of static boolean useDirectBuffer.
 * 2. Extra methods asIoBuffer, asShared, asUnshared, isShared.
 * 3. Remove static methods (mostly involving allocation, e.g. set/getAllocator, allocate, wrap)
 * 5. capacity, expand and shrink methods take an allocator parameter
 */

package org.kaazing.mina.core.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.IoBufferAllocator;

public interface IoBufferEx {

    int FLAG_NONE      = 0x00;
    int FLAG_READONLY  = 0x01 << 0;
    int FLAG_SHARED    = 0x01 << 1;
    int FLAG_DIRECT    = 0x01 << 2;
    int FLAG_ZERO_COPY = 0x01 << 3;

    IoBuffer asIoBuffer();

    int flags();

    boolean isShared();

    IoBufferEx asSharedBuffer();

    IoBufferEx asUnsharedBuffer();

    /**
     * Declares this buffer and all its derived buffers are not used anymore
     * so that it can be reused by some {@link IoBufferAllocator} implementations.
     * It is not mandatory to call this method, but you might want to invoke this
     * method for maximum performance.
     */
    void free();

    /**
     * Returns the underlying NIO buffer instance.
     */
    ByteBuffer buf();

    /**
     * @see ByteBuffer#isDirect()
     */
    boolean isDirect();

    /**
     * returns <tt>true</tt> if and only if this buffer is derived from other buffer
     * via {@link #duplicate()}, {@link #slice()} or {@link #asReadOnlyBuffer()}.
     */
    boolean isDerived();

    /**
     * @see ByteBuffer#isReadOnly()
     */
    boolean isReadOnly();

    /**
     * Returns the minimum capacity of this buffer which is used to determine
     * the new capacity of the buffer shrunk by {@link #compact()} and
     * {@link #shrink()} operation.  The default value is the initial capacity
     * of the buffer.
     */
    int minimumCapacity();

    /**
     * Sets the minimum capacity of this buffer which is used to determine
     * the new capacity of the buffer shrunk by {@link #compact()} and
     * {@link #shrink()} operation.  The default value is the initial capacity
     * of the buffer.
     */
    IoBufferEx minimumCapacity(int minimumCapacity);

    /**
     * @see ByteBuffer#capacity()
     */
    int capacity();

    /**
     * Increases the capacity of this buffer.  If the new capacity is less than
     * or equal to the current capacity, this method returns silently.  If the
     * new capacity is greater than the current capacity, the buffer is
     * reallocated while retaining the position, limit, mark and the content
     * of the buffer.
     */
    IoBufferEx capacity(int newCapacity, IoBufferAllocatorEx<?> reallocator);

    /**
     * Returns <tt>true</tt> if and only if <tt>autoExpand</tt> is turned on.
     */
    boolean isAutoExpand();

    /**
     * Turns on or off <tt>autoExpand</tt>.
     */
//    IoBufferEx setAutoExpand(boolean autoExpand);

    IoBufferEx setAutoExpander(IoBufferAllocatorEx<?> autoExpander);

    /**
     * Returns <tt>true</tt> if and only if <tt>autoShrink</tt> is turned on.
     */
    boolean isAutoShrink();

    /**
     * Turns on or off <tt>autoShrink</tt>.
     */
    IoBufferEx setAutoShrinker(IoBufferAllocatorEx<?> autoShrinker);

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the current position.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    IoBufferEx expand(int expectedRemaining, IoBufferAllocatorEx<?> expander);

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the specified
     * <tt>position</tt>.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    IoBufferEx expand(int position, int expectedRemaining, IoBufferAllocatorEx<?> expander);

    /**
     * Changes the capacity of this buffer so this buffer occupies as less
     * memory as possible while retaining the position, limit and the
     * buffer content between the position and limit.  The capacity of the
     * buffer never becomes less than {@link #minimumCapacity()}.
     * The mark is discarded once the capacity changes.
     */
    IoBufferEx shrink(IoBufferAllocatorEx<?> shrinker);

    /**
     * @see java.nio.Buffer#position()
     */
    int position();

    /**
     * @see java.nio.Buffer#position(int)
     */
    IoBufferEx position(int newPosition);

    /**
     * @see java.nio.Buffer#limit()
     */
    int limit();

    /**
     * @see java.nio.Buffer#limit(int)
     */
    IoBufferEx limit(int newLimit);

    /**
     * @see java.nio.Buffer#mark()
     */
    IoBufferEx mark();

    /**
     * Returns the position of the current mark.  This method returns <tt>-1</tt> if no
     * mark is set.
     */
    int markValue();

    /**
     * @see java.nio.Buffer#reset()
     */
    IoBufferEx reset();

    /**
     * @see java.nio.Buffer#clear()
     */
    IoBufferEx clear();

    /**
     * Clears this buffer and fills its content with <tt>NUL</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    IoBufferEx sweep();

    /**double
     * Clears this buffer and fills its content with <tt>value</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    IoBufferEx sweep(byte value);

    /**
     * @see java.nio.Buffer#flip()
     */
    IoBufferEx flip();

    /**
     * @see java.nio.Buffer#rewind()
     */
    IoBufferEx rewind();

    /**
     * @see java.nio.Buffer#remaining()
     */
    int remaining();

    /**
     * @see java.nio.Buffer#hasRemaining()
     */
    boolean hasRemaining();

    /**
     * @see ByteBuffer#duplicate()
     */
    IoBufferEx duplicate();

    /**
     * @see ByteBuffer#slice()
     */
    IoBufferEx slice();

    /**
     * @see ByteBuffer#asReadOnlyBuffer()
     */
    IoBufferEx asReadOnlyBuffer();

    /**
     * @see ByteBuffer#hasArray()
     */
    boolean hasArray();

    /**
     * @see ByteBuffer#array()
     */
    byte[] array();

    /**
     * @see ByteBuffer#arrayOffset()
     */
    int arrayOffset();

    /**
     * @see ByteBuffer#get()
     */
    byte get();

    /**
     * Reads one unsigned byte as a short integer.
     */
    short getUnsigned();

    /**
     * @see ByteBuffer#put(byte)
     */
    IoBufferEx put(byte b);

    /**
     * @see ByteBuffer#get(int)
     */
    byte get(int index);

    /**
     * Reads one byte as an unsigned short integer.
     */
    short getUnsigned(int index);

    /**
     * @see ByteBuffer#put(int, byte)
     */
    IoBufferEx put(int index, byte b);

    /**
     * @see ByteBuffer#get(byte[], int, int)
     */
    IoBufferEx get(byte[] dst, int offset, int length);

    /**
     * @see ByteBuffer#get(byte[])
     */
    IoBufferEx get(byte[] dst);

    /**
     * TODO document me.
     */
    IoBufferEx getSlice(int index, int length);

    /**
     * TODO document me.
     */
    IoBufferEx getSlice(int length);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    IoBufferEx put(ByteBuffer src);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    IoBufferEx put(IoBufferEx src);

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    IoBufferEx put(byte[] src, int offset, int length);

    /**
     * @see ByteBuffer#put(byte[])
     */
    IoBufferEx put(byte[] src);

    /**
     * @see ByteBuffer#compact()
     */
    IoBufferEx compact();

    /**
     * @see ByteBuffer#order()
     */
    ByteOrder order();

    /**
     * @see ByteBuffer#order(ByteOrder)
     */
    IoBufferEx order(ByteOrder bo);

    /**
     * @see ByteBuffer#getChar()
     */
    char getChar();

    /**
     * @see ByteBuffer#putChar(char)
     */
    IoBufferEx putChar(char value);

    /**
     * @see ByteBuffer#getChar(int)
     */
    char getChar(int index);

    /**
     * @see ByteBuffer#putChar(int, char)
     */
    IoBufferEx putChar(int index, char value);

    /**
     * @see ByteBuffer#asCharBuffer()
     */
    CharBuffer asCharBuffer();

    /**
     * @see ByteBuffer#getShort()
     */
    short getShort();

    /**
     * Reads two bytes unsigned integer.
     */
    int getUnsignedShort();

    /**
     * @see ByteBuffer#putShort(short)
     */
    IoBufferEx putShort(short value);

    /**
     * @see ByteBuffer#getShort()
     */
    short getShort(int index);

    /**
     * Reads two bytes unsigned integer.
     */
    int getUnsignedShort(int index);

    /**
     * @see ByteBuffer#putShort(int, short)
     */
    IoBufferEx putShort(int index, short value);

    /**
     * @see ByteBuffer#asShortBuffer()
     */
    ShortBuffer asShortBuffer();

    /**
     * @see ByteBuffer#getInt()
     */
    int getInt();

    /**
     * Reads four bytes unsigned integer.
     */
    long getUnsignedInt();

    /**
     * Relative <i>get</i> method for reading a medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order,
     * and then increments the position by three.</p>
     *
     * @return  The medium int value at the buffer's current position
     */
    int getMediumInt();

    /**
     * Relative <i>get</i> method for reading an unsigned medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order,
     * and then increments the position by three.</p>
     *
     * @return  The unsigned medium int value at the buffer's current position
     */
    int getUnsignedMediumInt();

    /**
     * Absolute <i>get</i> method for reading a medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order.</p>
     *
     * @param index  The index from which the medium int will be read
     * @return  The medium int value at the given index
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     */
    int getMediumInt(int index);

    /**
     * Absolute <i>get</i> method for reading an unsigned medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order.</p>
     *
     * @param index  The index from which the unsigned medium int will be read
     * @return  The unsigned medium int value at the given index
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     */
    int getUnsignedMediumInt(int index);

    /**
     * Relative <i>put</i> method for writing a medium int
     * value.
     *
     * <p> Writes three bytes containing the given int value, in the
     * current byte order, into this buffer at the current position, and then
     * increments the position by three.</p>
     *
     * @param  value
     *         The medium int value to be written
     *
     * @return  This buffer
     *
     * @throws  BufferOverflowException
     *          If there are fewer than three bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    IoBufferEx putMediumInt(int value);

    /**
     * Absolute <i>put</i> method for writing a medium int
     * value.
     *
     * <p> Writes three bytes containing the given int value, in the
     * current byte order, into this buffer at the given index.</p>
     *
     * @param  index
     *         The index at which the bytes will be written
     *
     * @param  value
     *         The medium int value to be written
     *
     * @return  This buffer
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit,
     *          minus three
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    IoBufferEx putMediumInt(int index, int value);

    /**
     * @see ByteBuffer#putInt(int)
     */
    IoBufferEx putInt(int value);

    /**
     * @see ByteBuffer#getInt(int)
     */
    int getInt(int index);

    /**
     * Reads four bytes unsigned integer.
     */
    long getUnsignedInt(int index);

    /**
     * @see ByteBuffer#putInt(int, int)
     */
    IoBufferEx putInt(int index, int value);

    /**
     * @see ByteBuffer#asIntBuffer()
     */
    IntBuffer asIntBuffer();

    /**
     * @see ByteBuffer#getLong()
     */
    long getLong();

    /**
     * @see ByteBuffer#putLong(int, long)
     */
    IoBufferEx putLong(long value);

    /**
     * @see ByteBuffer#getLong(int)
     */
    long getLong(int index);

    /**
     * @see ByteBuffer#putLong(int, long)
     */
    IoBufferEx putLong(int index, long value);

    /**
     * @see ByteBuffer#asLongBuffer()
     */
    LongBuffer asLongBuffer();

    /**
     * @see ByteBuffer#getFloat()
     */
    float getFloat();

    /**
     * @see ByteBuffer#putFloat(float)
     */
    IoBufferEx putFloat(float value);

    /**
     * @see ByteBuffer#getFloat(int)
     */
    float getFloat(int index);

    /**
     * @see ByteBuffer#putFloat(int, float)
     */
    IoBufferEx putFloat(int index, float value);

    /**
     * @see ByteBuffer#asFloatBuffer()
     */
    FloatBuffer asFloatBuffer();

    /**
     * @see ByteBuffer#getDouble()
     */
    double getDouble();

    /**
     * @see ByteBuffer#putDouble(double)
     */
    IoBufferEx putDouble(double value);

    /**
     * @see ByteBuffer#getDouble(int)
     */
    double getDouble(int index);

    /**
     * @see ByteBuffer#putDouble(int, double)
     */
    IoBufferEx putDouble(int index, double value);

    /**
     * @see ByteBuffer#asDoubleBuffer()
     */
    DoubleBuffer asDoubleBuffer();

    /**
     * Returns an {@link InputStream} that reads the data from this buffer.
     * {@link InputStream#read()} returns <tt>-1</tt> if the buffer position
     * reaches to the limit.
     */
    InputStream asInputStream();

    /**
     * Returns an {@link OutputStream} that appends the data into this buffer.
     * Please note that the {@link OutputStream#write(int)} will throw a
     * {@link BufferOverflowException} instead of an {@link IOException}
     * in case of buffer overflow.  Please set <tt>autoExpand</tt> property by
     * calling {@link #setAutoExpand(boolean)} to prevent the unexpected runtime
     * exception.
     */
    OutputStream asOutputStream();

    /**
     * Returns hexdump of this buffer.  The data and pointer are
     * not changed as a result of this method call.
     *
     * @return
     *  hexidecimal representation of this buffer
     */
    String getHexDump();

    /**
     * Return hexdump of this buffer with limited length.
     *
     * @param lengthLimit The maximum number of bytes to dump from
     *                    the current buffer position.
     * @return
     *  hexidecimal representation of this buffer
     */
    String getHexDump(int lengthLimit);

    ////////////////////////////////
    // String getters and putters //
    ////////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.  This method reads
     * until the limit of this buffer if no <tt>NUL</tt> is found.
     */
    String getString(CharsetDecoder decoder)
            throws CharacterCodingException;

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     *
     * @param fieldSize the maximum number of bytes to read
     */
    String getString(int fieldSize, CharsetDecoder decoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>.  This method doesn't terminate
     * string with <tt>NUL</tt>.  You have to do it by yourself.
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    IoBufferEx putString(CharSequence val, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify
     * odd <code>fieldSize</code>, and this method will append two
     * <code>NUL</code>s as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code>
     * if the input string is longer than <tt>fieldSize</tt>.
     *
     * @param fieldSize the maximum number of bytes to write
     */
    IoBufferEx putString(CharSequence val, int fieldSize,
            CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Reads a string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>decoder</code> and returns it.
     * This method is a shortcut for <tt>getPrefixedString(2, decoder)</tt>.
     */
    String getPrefixedString(CharsetDecoder decoder)
            throws CharacterCodingException;

    /**
     * Reads a string which has a length field before the actual
     * encoded string, using the specified <code>decoder</code> and returns it.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     */
    String getPrefixedString(int prefixLength, CharsetDecoder decoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, 2, 0, encoder)</tt>.
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    IoBufferEx putPrefixedString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, prefixLength, 0, encoder)</tt>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    IoBufferEx putPrefixedString(CharSequence in, int prefixLength,
            CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, prefixLength, padding, ( byte ) 0, encoder)</tt>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     * @param padding      the number of padded <tt>NUL</tt>s (1 (or 0), 2, or 4)
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    IoBufferEx putPrefixedString(CharSequence in, int prefixLength,
            int padding, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     * @param padding      the number of padded bytes (1 (or 0), 2, or 4)
     * @param padValue     the value of padded bytes
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    IoBufferEx putPrefixedString(CharSequence val, int prefixLength,
            int padding, byte padValue, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Reads a Java object from the buffer using the context {@link ClassLoader}
     * of the current thread.
     */
    Object getObject() throws ClassNotFoundException;

    /**
     * Reads a Java object from the buffer using the specified <tt>classLoader</tt>.
     */
    Object getObject(final ClassLoader classLoader)
            throws ClassNotFoundException;

    /**
     * Writes the specified Java object to the buffer.
     */
    IoBufferEx putObject(Object o);

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field.  This method is identical with
     * <tt>prefixedDataAvailable( prefixLength, Integer.MAX_VALUE )</tt>.
     * Please not that using this method can allow DoS (Denial of Service)
     * attack in case the remote peer sends too big data length value.
     * It is recommended to use {@link #prefixedDataAvailable(int, int)}
     * instead.
     *
     * @param prefixLength the length of the prefix field (1, 2, or 4)
     *
     * @throws IllegalArgumentException if prefixLength is wrong
     * @throws BufferDataException      if data length is negative
     */
    boolean prefixedDataAvailable(int prefixLength);

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field.
     *
     * @param prefixLength  the length of the prefix field (1, 2, or 4)
     * @param maxDataLength the allowed maximum of the read data length
     *
     * @throws IllegalArgumentException if prefixLength is wrong
     * @throws BufferDataException      if data length is negative or greater then <tt>maxDataLength</tt>
     */
    boolean prefixedDataAvailable(int prefixLength, int maxDataLength);

    /////////////////////
    // IndexOf methods //
    /////////////////////

    /**
     * Returns the first occurence position of the specified byte from the current position to
     * the current limit.
     *
     * @return <tt>-1</tt> if the specified byte is not found
     */
    int indexOf(byte b);

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    IoBufferEx skip(int size);

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    IoBufferEx fill(byte value, int size);

    /**
     * Fills this buffer with the specified value.
     * This method does not change buffer position.
     */
    IoBufferEx fillAndReset(byte value, int size);

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method moves buffer position forward.
     */
    IoBufferEx fill(int size);

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method does not change buffer position.
     */
    IoBufferEx fillAndReset(int size);

    //////////////////////////
    // Enum methods         //
    //////////////////////////

    /**
     * Reads a byte from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnum(Class<E> enumClass);

    /**
     * Reads a byte from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the byte will be read
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnum(int index, Class<E> enumClass);

    /**
     * Reads a short from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnumShort(Class<E> enumClass);

    /**
     * Reads a short from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the bytes will be read
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass);

    /**
     * Reads an int from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnumInt(Class<E> enumClass);

    /**
     * Reads an int from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the bytes will be read
     * @param enumClass  The enum's class object
     */
    <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass);

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     *
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnum(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     *
     * @param index The index at which the byte will be written
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnum(int index, Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     *
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnumShort(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     *
     * @param index The index at which the bytes will be written
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnumShort(int index, Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     *
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnumInt(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     *
     * @param index The index at which the bytes will be written
     * @param e  The enum to write to the buffer
     */
    IoBufferEx putEnumInt(int index, Enum<?> e);

    //////////////////////////
    // EnumSet methods      //
    //////////////////////////

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     *
     * <p>Each bit is mapped to a value in the specified enum.  The least significant
     * bit maps to the first entry in the specified enum and each subsequent bit maps
     * to each subsequent bit as mapped to the subsequent enum value.</p>
     *
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass);

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the byte will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSet(int index,
            Class<E> enumClass);

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass);

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index,
            Class<E> enumClass);

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass);

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index,
            Class<E> enumClass);

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass);

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index,
            Class<E> enumClass);

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSet(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the byte will be written
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSet(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetShort(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetShort(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetInt(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetInt(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetLong(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    <E extends Enum<E>> IoBufferEx putEnumSetLong(int index, Set<E> set);

}
