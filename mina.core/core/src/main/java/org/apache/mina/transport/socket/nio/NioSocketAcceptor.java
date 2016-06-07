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
/** The copyright above pertains to portions created by Kaazing */

package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.polling.AbstractPollingIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class NioSocketAcceptor
        extends AbstractPollingIoAcceptor<NioSession, ServerSocketChannel>
        implements SocketAcceptor {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NioSocketAcceptor.class);
    /** 
     * Define the number of socket that can wait to be accepted. Default
     * to 50 (as in the SocketServer default).
     */
    private int backlog = 50;

    private boolean reuseAddress = false;

    private volatile Selector selector;

    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters (multiple thread model).
     */
    public NioSocketAcceptor() {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters, and 
     * given number of {@link NioProcessor} for multithreading I/O operations.
     * 
     * @param processorCount the number of processor to create and place in a
     * {@link SimpleIoProcessorPool} 
     */
    public NioSocketAcceptor(int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
    *  Constructor for {@link NioSocketAcceptor} with default configuration but a
     *  specific {@link IoProcessor}, useful for sharing the same processor over multiple
     *  {@link org.apache.mina.core.service.IoService} of the same type.
     * @param processor the processor to use for managing I/O events
     */
    public NioSocketAcceptor(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     *  Constructor for {@link NioSocketAcceptor} with a given {@link Executor} for handling 
     *  connection events and a given {@link IoProcessor} for handling I/O events, useful for 
     *  sharing the same processor and executor over multiple {@link org.apache.mina.core.service.IoService} of the same type.
     * @param executor the executor for connection
     * @param processor the processor for I/O operations
     */
    public NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {
        selector = Selector.open();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "reuseAddress can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * {@inheritDoc}
     */
    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NioSession accept(IoProcessor<NioSession> processor,
            ServerSocketChannel handle) throws Exception {

        SelectionKey key = handle.keyFor(selector);
        
        if ((key == null) || (!key.isValid()) || (!key.isAcceptable()) ) {
            return null;
        }

        // accept the connection from the client
        SocketChannel ch = handle.accept();
        
        if (ch == null) {
            return null;
        }

        final NioSocketSession session = new NioSocketSession(this, processor, ch);

        try {
            session.initSessionConfig();
            return session;
        } catch (RuntimeIoException e) {
            if ( AbstractIoSessionConfig.REPORT_SESSION_CONFIG_FAILURE) {
                throw e;
            }
            if ( LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unexpected exception initializing session " +
                             "TCP options. Rejecting connection.", e);
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServerSocketChannel open(SocketAddress localAddress)
            throws Exception {
        // Creates the listening ServerSocket
        ServerSocketChannel channel = ServerSocketChannel.open();
        
        boolean success = false;
        
        try {
            // This is a non blocking socket channel
            channel.configureBlocking(false);
        
            // Configure the server socket,
            ServerSocket socket = channel.socket();
            
            // Set the reuseAddress flag accordingly with the setting
            socket.setReuseAddress(isReuseAddress());
            
            if (AbstractIoSessionConfig.ENABLE_BUFFER_SIZE) {
                System.out.println("NioSocketAcceptor.open(): setReceiveBufferSize:"+getSessionConfig().getReceiveBufferSize());
                // XXX: Do we need to provide this property? (I think we need to remove it.)
                socket.setReceiveBufferSize(getSessionConfig().getReceiveBufferSize());
            }
            
            // and bind.
            socket.bind(localAddress, getBacklog());
            
            // Register the channel within the selector for ACCEPT event
            channel.register(selector, SelectionKey.OP_ACCEPT);
            success = true;
        } finally {
            if (!success) {
                close(channel);
            }
        }
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle)
            throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    /**
      * Check if we have at least one key whose corresponding channels is 
      * ready for I/O operations.
      *
      * This method performs a blocking selection operation. 
      * It returns only after at least one channel is selected, 
      * this selector's wakeup method is invoked, or the current thread 
      * is interrupted, whichever comes first.
      * 
      * @return The number of keys having their ready-operation set updated
      * @throws java.io.IOException If an I/O error occurs
      * @throws java.nio.channels.ClosedSelectorException If this selector is closed
      */
    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<ServerSocketChannel> selectedHandles() {
        return new ServerSocketChannelIterator(selector.selectedKeys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void close(ServerSocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);
        
        if (key != null) {
            key.cancel();
        }
        
        handle.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    /**
     * Defines an iterator for the selected-key Set returned by the 
     * selector.selectedKeys(). It replaces the SelectionKey operator.
     */
    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {
        /** The selected-key iterator */
        private final Iterator<SelectionKey> iterator;

        /**
         * Build a SocketChannel iterator which will return a SocketChannel instead of
         * a SelectionKey.
         * 
         * @param selectedKeys The selector selected-key set 
         */
        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            iterator = selectedKeys.iterator();
        }

        /**
         * Tells if there are more SockectChannel left in the iterator
         * @return <code>true</code> if there is at least one more 
         * SockectChannel object to read
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * Get the next SocketChannel in the operator we have built from
         * the selected-key et for this selector.
         * 
         * @return The next SocketChannel in the iterator
         */
        public ServerSocketChannel next() {
            SelectionKey key = iterator.next();
            
            if ( key.isValid() && key.isAcceptable() ) {
                return (ServerSocketChannel) key.channel();
            }

            return null;
        }

        /**
         * Remove the current SocketChannel from the iterator 
         */
        public void remove() {
            iterator.remove();
        }
    }
}
