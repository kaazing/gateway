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
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.AbstractSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class NioSocketSession extends NioSession {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "nio", "socket", false, true,
                    InetSocketAddress.class,
                    SocketSessionConfig.class,
                    IoBuffer.class, FileRegion.class);

    private final IoService service;

    private final SocketSessionConfig config = new SessionConfigImpl();

    private final IoProcessor<NioSession> processor;

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);

    private final SocketChannel ch;

    private final IoHandler handler;

    private SelectionKey key;

    
    /**
     * 
     * Creates a new instance of NioSocketSession.
     *
     * @param service the associated IoService 
     * @param processor the associated IoProcessor
     * @param ch the used channel
     */
    public NioSocketSession(IoService service, IoProcessor<NioSession> processor, SocketChannel ch) {
        this.service = service;
        this.processor = processor;
        this.ch = ch;
        this.handler = service.getHandler();
    }

    public void initSessionConfig() throws RuntimeIoException {
        this.config.setAll(service.getSessionConfig());
    }

    public IoService getService() {
        return service;
    }

    public SocketSessionConfig getConfig() {
        return config;
    }

    @Override
    public IoProcessor<NioSession> getProcessor() {
        return processor;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    @Override
    SocketChannel getChannel() {
        return ch;
    }

    @Override
    SelectionKey getSelectionKey() {
        return key;
    }

    @Override
    void setSelectionKey(SelectionKey key) {
        this.key = key;
    }

    public IoHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getRemoteAddress() {
        if ( ch == null ) {
            return null;
        }
        
        Socket socket = ch.socket();
        
        if ( socket == null ) {
            return null;
        }
        
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress() {
        if ( ch == null ) {
            return null;
        }
        
        Socket socket = ch.socket();
        
        if ( socket == null ) {
            return null;
        }
        
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    private class SessionConfigImpl extends AbstractSocketSessionConfig {
        public boolean isKeepAlive() {
            try {
                return ch.socket().getKeepAlive();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setKeepAlive(boolean on) {
            try {
                ch.socket().setKeepAlive(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isOobInline() {
            try {
                return ch.socket().getOOBInline();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setOobInline(boolean on) {
            try {
                ch.socket().setOOBInline(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isReuseAddress() {
            try {
                return ch.socket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReuseAddress(boolean on) {
            try {
                ch.socket().setReuseAddress(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSoLinger() {
            try {
                return ch.socket().getSoLinger();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSoLinger(int linger) {
            try {
                if (linger < 0) {
                    ch.socket().setSoLinger(false, 0);
                } else {
                    ch.socket().setSoLinger(true, linger);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isTcpNoDelay() {
            if (!isConnected()) {
                return false;
            }

            try {
                return ch.socket().getTcpNoDelay();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setTcpNoDelay(boolean on) {
            try {
                ch.socket().setTcpNoDelay(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getTrafficClass() {
            try {
                return ch.socket().getTrafficClass();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setTrafficClass(int tc) {
            try {
                ch.socket().setTrafficClass(tc);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSendBufferSize() {
            try {
                return ch.socket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSendBufferSize(int size) {
            try {
                if (AbstractIoSessionConfig.ENABLE_BUFFER_SIZE) {
                    System.out.println("NioSocketSession.setSendBufferSize("+size+")");
                    ch.socket().setSendBufferSize(size);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getReceiveBufferSize() {
            try {
                return ch.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReceiveBufferSize(int size) {
            try {
                if (AbstractIoSessionConfig.ENABLE_BUFFER_SIZE) {
                    System.out.println("NioSocketSession.setReceiveBufferSize("+size+")");
                    ch.socket().setReceiveBufferSize(size);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }
}
