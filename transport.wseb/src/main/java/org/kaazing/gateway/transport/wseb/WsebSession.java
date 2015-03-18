/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wseb;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.ActiveWsExtensions;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsebSession extends AbstractWsBridgeSession<WsebSession, WsBuffer> {

    private static final boolean ALIGN_DOWNSTREAM = Boolean.parseBoolean(System.getProperty("org.kaazing.gateway.transport.wseb.ALIGN_DOWNSTREAM", "true"));
    private static final boolean ALIGN_UPSTREAM = Boolean.parseBoolean(System.getProperty("org.kaazing.gateway.transport.wseb.ALIGN_UPSTREAM", "true"));
    
    static final CachingMessageEncoder WSEB_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("wseb", encoder, message, allocator, flags);
        }

    };

    static final CachingMessageEncoder WSEB_MESSAGE_ESCAPE_ZERO_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("wseb-escape0", encoder, message, allocator, flags);
        }

    };
    
    static final CachingMessageEncoder WSEB_MESSAGE_ESCAPE_ZERO_AND_NEWLINE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("wseb-escape", encoder, message, allocator, flags);
        }

    };
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WsebSession.class);
    private static final WriteRequest PING_REQUEST = new DefaultWriteRequestEx(new Object());
    private static final WriteRequest PONG_REQUEST = new DefaultWriteRequestEx(new Object());
    private static final WriteRequest RECONNECT_REQUEST = new DefaultWriteRequestEx(new Object());

    private final AtomicBoolean attachingWrite;
    private final AtomicReference<IoSessionEx> readSession;
    private final AtomicReference<IoSessionEx> pendingNewWriter;
    private final TimeoutCommand timeout;
    private final int clientIdleTimeout;
    private final long inactivityTimeout;

    private EscapeTypes encodeEscapeType = EscapeTypes.NO_ESCAPE;
    private ResourceAddress readAddress;
    private ResourceAddress writeAddress;

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    
    private final Runnable enqueueReconnectAndFlushTask = new Runnable() {
        @Override public void run() {
            enqueueReconnectAndFlush0();
        }
    };

    public WsebSession(int ioLayer,
                       Thread ioThread, 
                       Executor ioExecutor, 
                       IoServiceEx service,
                       IoProcessorEx<WsebSession> processor, 
                       ResourceAddress localAddress, 
                       ResourceAddress remoteAddress, 
                       IoBufferAllocatorEx<WsBuffer> allocator, 
                       DefaultLoginResult loginResult,
                       ActiveWsExtensions wsExtensions, 
                       int clientIdleTimeout,
                       long inactivityTimeout) {
        super(ioLayer,
              ioThread,
              ioExecutor,
              service,
              processor,
              localAddress,
              remoteAddress,
              allocator,
              Direction.BOTH,
              loginResult,
              wsExtensions);
        this.attachingWrite = new AtomicBoolean(false);
        this.readSession = new AtomicReference<IoSessionEx>();
        this.pendingNewWriter = new AtomicReference<IoSessionEx>();
        this.timeout = new TimeoutCommand(this);
        this.clientIdleTimeout = clientIdleTimeout;
        this.inactivityTimeout = inactivityTimeout;
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        switch(this.encodeEscapeType) {
        case ESCAPE_ZERO_AND_NEWLINES:
            return WSEB_MESSAGE_ESCAPE_ZERO_AND_NEWLINE_ENCODER;
        case ESCAPE_ZERO:
            return WSEB_MESSAGE_ESCAPE_ZERO_ENCODER;
        default:
            return WSEB_MESSAGE_ENCODER;
        }
    }

    public Runnable getTimeoutCommand() {
        return timeout;
    }

    public void clearTimeoutCommand() {
        timeout.clear();
    }

    public void setReadAddress(ResourceAddress readAddress) {
        this.readAddress = readAddress;
    }

    public ResourceAddress getReadAddress() {
        return readAddress;
    }

    public void setWriteAddress(ResourceAddress writeAddress) {
        this.writeAddress = writeAddress;
    }

    public ResourceAddress getWriteAddress() {
        return writeAddress;
    }

    /**
     * Attach new writer immediately if there is none. Or, if there already is one, enqueue a request to
     * switch to the new writer, which will be done by WsebAcceptProcessor.flushInternal (this avoids
     * races between that method and this one, see KG-2756).
     * @param newWriter
     */
    public void attachWriter(final IoSessionEx newWriter) {
        // The attachWriter processing must be done in this WsebSession's IO thread so we can do
        // getProcessor().flush(). We may need to do "thread hopping" for this since attachWriter gets called by
        // WsebDownstreamHandler.reconnectSession during sessionOpened on the downstream, which may be running
        // in another I/O thread.
        if (Thread.currentThread() == getIoThread()) {
            attachWriter0(newWriter);
        }
        else {
            if (ALIGN_DOWNSTREAM) {
                final Thread ioThread = getIoThread();
                final Executor ioExecutor = getIoExecutor();
                newWriter.setIoAlignment(NO_THREAD, NO_EXECUTOR);
                ioExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        newWriter.setIoAlignment(ioThread, ioExecutor);
                        attachWriter0(newWriter);
                    }
                });
            }
            else {
                getIoExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        attachWriter0(newWriter);
                    }
                });
            }
        }
    }

    private void attachWriter0(final IoSessionEx newWriter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("attachWriter on WsebSession wseb#%d, newWriter=%s", this.getId(), newWriter));
        }
        reconnecting.set(false);
        if (!isClosing()) {
            if (!compareAndSetParent(null, newWriter)) {
                // There's an existing parent (writer). Enqueue a request to switch to new writer.
                IoSessionEx oldPending = pendingNewWriter.getAndSet(newWriter);
                if (oldPending != null) {
                    // Unlikely (means client established two new downstreams in rapid succession without
                    // receiving any data on the first one) but better safe than sorry.
                    oldPending.close(false);
                }
                enqueueReconnectRequest();
                // Do not return here, need to flush (below) to make sure the old downstream gets closed
                // even if there is no more downstream data being sent (KG-4384)
            } else {
                if (Long.valueOf(0L).equals(newWriter.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY))) {
                    // long-polling case, need to buffer so that Content-Length is written
                    newWriter.suspendWrite();
                }
            }

            if (!isWriteSuspended()) {
                getProcessor().flush(this);
            }
        }
        else {
            if (newWriter != null) {
                newWriter.close(false);
            }
        }
        attachingWrite.set(false);

        //
        // Now that we have set up a parent for the session,
        // is the time to start scheduled commands which may need
        // to add their own filters to the parent's filter chain.
        //
        if ( !isClosing() ) {
            try {
                this.startupScheduledCommands();
            } catch (Exception e) {
                logger.error("Failed to start background commands for session", e);
                throw new RuntimeException(e);
            }
        }
    }

    boolean detachWriter(final HttpSession oldWriter) {
        boolean detached = compareAndSetParent(oldWriter, null);

        if (detached && Long.valueOf(0L).equals(oldWriter.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY))) {
            // long-polling case, writes are done (so end of buffering)
            oldWriter.shutdownWrite();
            oldWriter.resumeWrite();
        }

        if (Thread.currentThread() == getIoThread()) {
            detachWriter0(oldWriter);
        } else {
            if (ALIGN_DOWNSTREAM) {
                final Executor ioExecutor = getIoExecutor();
                ioExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        detachWriter0(oldWriter);
                    }
                });
            } else {
                detachWriter0(oldWriter);
            }
        }

        return detached;
    }

    private void detachWriter0(final HttpSession oldWriter) {
        if (oldWriter.getIoThread() == getIoThread()) {
            oldWriter.close(false);
        } else {
            final Executor ioExecutor = getIoExecutor();
            ioExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    detachWriter0(oldWriter);
                }
            });
        }
    }

    public void attachPendingWriter() {
        IoSessionEx pendingWriter = pendingNewWriter.getAndSet(null);
        if (pendingWriter != null) {
            attachWriter(pendingWriter);
        }
    }

    public void attachReader(final IoSessionEx newReader) {
        // The attachReader processing should be done in this WsebSession's IO thread so we can do
        // fireMessageReceived(). We may need to do "thread hopping" for this since attachReader gets called by
        // WsebUpstreamHandler during sessionOpened on the upstream, which may be running
        // in another I/O thread.
        if (Thread.currentThread() == getIoThread()) {
            attachReader0(newReader);
        }
        else {
            if (ALIGN_UPSTREAM) {
                final Thread ioThread = getIoThread();
                final Executor ioExecutor = getIoExecutor();
                newReader.setIoAlignment(NO_THREAD, NO_EXECUTOR);
                ioExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        newReader.setIoAlignment(ioThread, ioExecutor);
                        attachReader0(newReader);
                    }
                });
            }
            else {
                getIoExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        attachReader0(newReader);
                    }
                });
            }
        }
        
    }

    private void attachReader0(final IoSessionEx newReader) {
        // TODO: needs improved handling of old value for overlapping downstream
        //       from client perspective to detect buffering proxies
        // TODO: needs re-alignment similar to attachWriter
        readSession.set(newReader);
        if (this.isReadSuspended()) {
            newReader.suspendRead();
        }
    }
    
    public void enqueueReconnectAndFlush() {
        // The processing must be done in this WsebSession's IO thread so we can do getProcessor().flush().
        if (Thread.currentThread() == getIoThread()) {
            enqueueReconnectAndFlush0();
        }
        else {
            getIoExecutor().execute(enqueueReconnectAndFlushTask);
        }
    }

    private void enqueueReconnectAndFlush0() {
        enqueueReconnectRequest();

        // KG-5615: flush pending reconnect request for long-polling, where
        //          there may be no additional write to implicitly flush for us
        if (!isWriteSuspended()) {
            getProcessor().flush(WsebSession.this);
        }
    }

    public boolean detachReader(IoSessionEx oldReader) {
        return readSession.compareAndSet(oldReader, null);
    }

    public IoSessionEx getReader() {
        return readSession.get();
    }

    public IoSessionEx getWriter() {
        return getParent();
    }

    @Override
    public WriteFuture write(Object message) {
        return super.write(message);
    }

    public boolean compareAndSetAttachingWrite(boolean expected, boolean newValue) {
        return attachingWrite.compareAndSet(expected, newValue);
    }

    @Override
    protected void suspendRead1() {
        super.suspendRead2();

        IoSession readSession = this.readSession.get();
        if (readSession != null) {
            readSession.suspendRead();
        }
    }
    
    @Override
    protected void resumeRead1() {
        // call super first to trigger processor.consume()
        super.resumeRead2();

        IoSession readSession = this.readSession.get();
        if (readSession != null) {
            readSession.resumeRead();
        }
    }

    boolean compareAndSetReconnecting(boolean expected, boolean newValue) {
        return reconnecting.compareAndSet(expected, newValue);
    }

    void issuePingRequest() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("enqueuePingRequest on WsebSession %s", this));
        }
        WriteRequestQueue writeRequestQueue = getWriteRequestQueue();
        writeRequestQueue.offer(this, PING_REQUEST);
        if (!isWriteSuspended()) {
            getProcessor().flush(this);
        }
    }

    static boolean isPingRequest(WriteRequest request) {
        return request == PING_REQUEST;
    }

    void issuePongRequest() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("enqueuePongRequest on WsebSession %s", this));
        }
        WriteRequestQueue writeRequestQueue = getWriteRequestQueue();
        writeRequestQueue.offer(this, PONG_REQUEST);
        if (!isWriteSuspended()) {
            getProcessor().flush(this);
        }
    }

    static boolean isPongRequest(WriteRequest request) {
        return request == PONG_REQUEST;
    }

    void enqueueReconnectRequest() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("enqueueReconnectRequest on WsebSession %s", this));
        }
        WriteRequestQueue writeRequestQueue = getWriteRequestQueue();
        writeRequestQueue.offer(this, RECONNECT_REQUEST);
    }

    static boolean isReconnectRequest(WriteRequest request) {
        return request == RECONNECT_REQUEST;
    }

    boolean isReconnecting() {
        return reconnecting.get();
    }

    public int getClientIdleTimeout() {
        return clientIdleTimeout;
    }

    public long getInactivityTimeout() {
        return inactivityTimeout;
    }
    
    // close session if reconnect timer elapses and no parent has been attached
    private static class TimeoutCommand implements Runnable {

        private volatile WsebSession session;

        public TimeoutCommand(WsebSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            WsebSession session = this.session;
            if (session != null) {
                // technically if this is being called then we have passed the timeout and no reconnect
                // has happened because it would have canceled this task, but doing a check just in case of a race condition
                if (!session.isClosing()) {
                    IoSession parent = session.getParent();
                    if (parent == null) {
                        session.close(true);
                    }
                }
            }
        }

        public void clear() {
            session = null;
        }
    }

    public void setEncodeEscapeType(WsebEncodingCodecFilter.EscapeTypes escape) {
        this.encodeEscapeType  = escape;
        
    }


}
