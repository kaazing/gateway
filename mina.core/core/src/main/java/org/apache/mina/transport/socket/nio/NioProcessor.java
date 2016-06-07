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

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class NioProcessor extends AbstractPollingIoProcessor<NioSession> {
    /** The selector associated with this processor */
    private final Selector selector;

    /**
     * 
     * Creates a new instance of NioProcessor.
     *
     * @param executor
     */
    public NioProcessor(Executor executor) {
        super(executor);
        try {
            // Open a new selector
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
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
    protected Iterator<NioSession> allSessions() {
        return new IoSessionIterator(selector.keys());
    }

    @SuppressWarnings("synthetic-access")
    @Override
    protected Iterator<NioSession> selectedSessions() {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected void init(NioSession session) throws Exception {
        SelectableChannel ch = (SelectableChannel) session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    @Override
    protected void destroy(NioSession session) throws Exception {
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
    protected SessionState getState(NioSession session) {
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
    protected boolean isReadable(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isReadable();
    }

    @Override
    protected boolean isWritable(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_READ) != 0;
    }

    @Override
    protected boolean isInterestedInWrite(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(NioSession session, boolean isInterested) throws Exception {
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
    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
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
    protected int read(NioSession session, IoBuffer buf) throws Exception {
        return session.getChannel().read(buf.buf());
    }

    @Override
    protected int write(NioSession session, IoBuffer buf, int length) throws Exception {
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
    protected int transferFile(NioSession session, FileRegion region, int length) throws Exception {
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
    protected static class IoSessionIterator<NioSession> implements Iterator<NioSession> {
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
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public NioSession next() {
            SelectionKey key = iterator.next();
            NioSession nioSession =  (NioSession) key.attachment();
            return nioSession;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            iterator.remove();
        }
    }
}