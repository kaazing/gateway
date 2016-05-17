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
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.session.SessionState;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

/**
 * Used instead of Mina's NioProcessor to (a) support sessions implementing IoSessionEx and
 * (b) avoid unnecessary ByteBuffer duplication.
 */
public final class NioProcessorEx extends AbstractPollingIoProcessor<NioSessionEx> {
    /** The selector associated with this processor */
    private final Selector selector;

    /**
     *
     * Creates a new instance of NioProcessor.
     *
     * @param executor
     */
    public NioProcessorEx(Executor executor) {
        super(executor);
        try {
            // Open a new selector
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    @Override
    protected Object getWriteRequestMessage(NioSessionEx session, WriteRequest writeRequest) {

        // 1. lookup current write buffer
        IoBufferEx writeBuffer = session.getIncompleteSharedWriteBuffer();

        if (writeBuffer != null) {
            // 1a. buffer obtained from a previously shared, incomplete write
            assert !writeBuffer.isShared();
            return writeBuffer;
        }

        // 1b. current write is either unshared or first attempt
        return writeRequest.getMessage();
    }

    @Override
    protected int writeBuffer(NioSessionEx session, WriteRequest req,
            IoBuffer buf, boolean hasFragmentation, int maxLength,
            long currentTime) throws Exception {

        // empty buffers may not be IoBufferEx, see IoBuffer.allocate()
        if (!buf.hasRemaining()) {
            return super.writeBuffer(session, req, buf, hasFragmentation, maxLength, currentTime);
        }

        // 1. test if buffer is shared across sessions (could be same or different I/O thread)
        IoBufferEx bufEx = (IoBufferEx) buf;
        if (!bufEx.isShared()) {
            // 1a. buffer is not shared across sessions, typical behavior
            int remaining = buf.remaining();

            int localWrittenBytes = super.writeBuffer(session, req, buf, hasFragmentation, maxLength, currentTime);

            if (localWrittenBytes == remaining) {
                // 1b. previously shared, incomplete write is now complete, OR
                //     previously unshared, incomplete write is now complete, OR
                //     unshared write is now complete on first attempt
                session.setIncompleteSharedWriteBuffer(null);
            }

            return localWrittenBytes;
        }

        // 2. buffer is shared across sessions
        //    remember position in case of incomplete write
        //    access NIO buffer directly to minimize ThreadLocal lookups
        ByteBuffer nioBuf = buf.buf();
        int position = nioBuf.position();
        int remaining = nioBuf.remaining();

        int localWrittenBytes = super.writeBuffer(session, req, buf, hasFragmentation, maxLength, currentTime);

        // 3. detect shared buffer incomplete write
        if (localWrittenBytes < remaining) {
            // 3a. diverge from master shared buffer and reset master position as if fully written
            //     master is thread local, so changing position does not affect other threads
            IoBufferEx incomplete = bufEx.asUnsharedBuffer();
            session.setIncompleteSharedWriteBuffer(incomplete);
            nioBuf.position(position);
        }

        // 4. either shared write complete (on first attempt),
        //    or shared write incomplete and diverged to prevent side-effects on other sessions
        return localWrittenBytes;
    }

    @Override
    protected void dispose0() throws Exception {
        selector.close();
    }

    @Override
    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    @Override
    protected boolean isSelectorEmpty() {
        return selector.keys().isEmpty();
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    @Override
    protected Iterator<NioSessionEx> allSessions() {
        return new IoSessionIterator(selector.keys());
    }

    @SuppressWarnings("synthetic-access")
    @Override
    protected Iterator<NioSessionEx> selectedSessions() {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected void init(NioSessionEx session) throws Exception {
        SelectableChannel ch = (SelectableChannel) session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    @Override
    protected void destroy(NioSessionEx session) throws Exception {
        ByteChannel ch = session.getChannel();
        SelectionKey key = session.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        ch.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(NioSessionEx session) {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            // The channel is not yet registered to a selector
            return SessionState.OPENING;
        }

        if (key.isValid()) {
            // The session is opened
            return SessionState.OPENED;
        } else {
            // The session still as to be closed
            return SessionState.CLOSING;
        }
    }

    @Override
    protected boolean isReadable(NioSessionEx session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isReadable();
    }

    @Override
    protected boolean isWritable(NioSessionEx session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(NioSessionEx session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_READ) != 0;
    }

    @Override
    protected boolean isInterestedInWrite(NioSessionEx session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(NioSessionEx session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        int oldInterestOps = key.interestOps();
        int newInterestOps = oldInterestOps;

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_READ;
        }

        if (oldInterestOps != newInterestOps) {
            key.interestOps(newInterestOps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(NioSessionEx session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        int oldInterestOps = key.interestOps();
        int newInterestOps = oldInterestOps;

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }

        if (oldInterestOps != newInterestOps) {
            key.interestOps(newInterestOps);
        }
    }

    @Override
    protected IoBuffer newReadBuffer(int readBufferSize) {
        // note: this assumes NioSessionEx.getBufferAllocator() returns SimpleBufferAllocator.BUFFER_ALLOCATOR
        return SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(readBufferSize));
    }

    @Override
    protected int read(NioSessionEx session, IoBuffer buf) throws Exception {
        return session.getChannel().read(buf.buf());
    }

    @Override
    protected int write(NioSessionEx session, IoBuffer buf, int length) throws Exception {
        if (buf.remaining() <= length) {
            return session.getChannel().write(buf.buf());
        }

        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf.buf());
        } finally {
            buf.limit(oldLimit);
        }
    }

    @Override
    protected int transferFile(NioSessionEx session, FileRegion region, int length) throws Exception {
        try {
            return (int) region.getFileChannel().transferTo(region.getPosition(), length, session.getChannel());
        } catch (IOException e) {
            // Check to see if the IOException is being thrown due to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
            String message = e.getMessage();
            if (message != null && message.contains("temporarily unavailable")) {
                return 0;
            }

            throw e;
        }
    }

    /**
     * An encapsulating iterator around the  {@link Selector#selectedKeys()}
     * or the {@link Selector#keys()} iterator;
     */
    protected static final class IoSessionIterator implements Iterator<NioSessionEx> {
        private final Iterator<SelectionKey> iterator;

        /**
         * Create this iterator as a wrapper on top of the selectionKey
         * Set.
         * @param keys The set of selected sessions
         */
        private IoSessionIterator(Set<SelectionKey> keys) {
            iterator = keys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NioSessionEx next() {
            SelectionKey key = iterator.next();
            NioSessionEx nioSession =  (NioSessionEx) key.attachment();
            return nioSession;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
