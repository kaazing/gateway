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

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kaazing.gateway.transport.wseb.WsebDownstreamHandler.TIME_TO_TIMEOUT_RECONNECT_MILLIS;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.DummySessionEx;
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
    private static final WriteRequest RECONNECT_REQUEST = new DefaultWriteRequestEx(new Object());

    private long readerSequenceNo;
    private long writerSequenceNo;

    private final AtomicBoolean attachingWrite;
    private final AtomicReference<IoSessionEx> readSession;
    private final AtomicReference<HttpSession> pendingNewWriter;
    private final TimeoutCommand timeout;
    private final int clientIdleTimeout;
    private final long inactivityTimeout;
    private final boolean validateSequenceNo;

    private boolean firstWriter = true;

    private EscapeTypes encodeEscapeType = EscapeTypes.NO_ESCAPE;
    private ResourceAddress readAddress;
    private ResourceAddress writeAddress;

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    private final Runnable enqueueReconnectAndFlushTask = new Runnable() {
        @Override public void run() {
            enqueueReconnectAndFlush0();
        }
    };
    private ScheduledFuture<?> timeoutFuture;

    private TransportSession transportSession;

    volatile Throwable writerException;

    public WsebSession(int ioLayer,
                       Thread ioThread,
                       Executor ioExecutor,
                       IoServiceEx service,
                       IoProcessorEx<WsebSession> processor,
                       ResourceAddress localAddress,
                       ResourceAddress remoteAddress,
                       IoBufferAllocatorEx<WsBuffer> allocator,
                       DefaultLoginResult loginResult,
                       int clientIdleTimeout,
                       long inactivityTimeout,
                       boolean validateSequenceNo,
                       long sequenceNo,
                       List<WebSocketExtension> extensions) {
        super(ioLayer,
              ioThread,
              ioExecutor,
              service,
              wsebSessionProcessor,
              localAddress,
              remoteAddress,
              allocator,
              Direction.BOTH,
              loginResult,
              extensions);
        this.attachingWrite = new AtomicBoolean(false);
        this.readSession = new AtomicReference<>();
        this.pendingNewWriter = new AtomicReference<>();
        this.timeout = new TimeoutCommand(this);
        this.clientIdleTimeout = clientIdleTimeout;
        this.inactivityTimeout = inactivityTimeout;
        this.validateSequenceNo = validateSequenceNo;
        this.readerSequenceNo = sequenceNo+1;
        this.writerSequenceNo = sequenceNo+1;
        this.transportSession = new TransportSession(this, processor);
        transportSession.setHandler(transportHandler);
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
    public void attachWriter(final HttpSession newWriter) {
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

    private void attachWriter0(final HttpSession newWriter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("attachWriter on WsebSession wseb#%d, newWriter=%s", this.getId(), newWriter));
        }
        reconnecting.set(false);
        if (!isClosing()) {
            if (!compareAndSetParent(null, newWriter)) {
                cancelTimeout();

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
                if (newWriter instanceof HttpAcceptSession) {
                    HttpAcceptSession newAcceptWriter = (HttpAcceptSession) newWriter;
                    // check if the writer is out of order
                    if (isLongPollingOutOfOrder(newAcceptWriter) || isWriterOutOfOrder(newAcceptWriter)) {
                        closeSession(newAcceptWriter);
                        return;
                    }
                    writeNoop((HttpAcceptSession) newWriter);
                }

                writerSequenceNo++;
                firstWriter = false;

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
            if (!oldWriter.isClosing()) {
                oldWriter.write(WsCommandMessage.RECONNECT);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("detachWriter on WsebSession wseb#%d, oldWriter=%s", this.getId(), oldWriter));
            }
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

    public boolean attachPendingWriter() {
        HttpSession pendingWriter = pendingNewWriter.getAndSet(null);
        if (pendingWriter != null) {
            attachWriter(pendingWriter);
            return true;
        }
        return false;
    }

    void scheduleTimeout(ScheduledExecutorService scheduler) {
        if (timeoutFuture == null || timeoutFuture.cancel(false)) {
            timeoutFuture = scheduler.schedule(timeout, TIME_TO_TIMEOUT_RECONNECT_MILLIS, MILLISECONDS);
        }
    }

    void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }


    public void attachReader(final HttpSession newReader) {
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
        if (newReader instanceof HttpAcceptSession) {
            HttpAcceptSession newAcceptReader = (HttpAcceptSession) newReader;
            if (isReaderOutOfOrder(newAcceptReader)) {
                closeSession(newAcceptReader);
                return;
            }
        }

        readerSequenceNo++;
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

    public HttpSession getWriter() {
        return (HttpSession)getParent();
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

    IoSessionEx getTransportSession() {
        return transportSession;
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
    }

    public void setEncodeEscapeType(WsebEncodingCodecFilter.EscapeTypes escape) {
        this.encodeEscapeType  = escape;

    }

    private boolean isWriterOutOfOrder(HttpAcceptSession session) {
        if (validateSequenceNo) {
            return isOutOfOrder(session, writerSequenceNo);
        }
        return false;
    }

    private boolean isReaderOutOfOrder(HttpAcceptSession session) {
        if (validateSequenceNo) {
            return isOutOfOrder(session, readerSequenceNo);
        }
        return false;
    }

    private boolean isOutOfOrder(HttpAcceptSession session, long expectedSequenceNo) {
        String sequenceNo = session.getReadHeader(HttpHeaders.HEADER_X_SEQUENCE_NO);

        if (sequenceNo == null || expectedSequenceNo != Long.parseLong(sequenceNo)) {
            if (LOGGER.isDebugEnabled()) {
                String logStr = String.format("Closing HTTP session [HTTP#%d], WSEB session [WSEB#%d] as an " +
                                "out of order request is received (expected seq no=%d, got=%s)", session.getId(),
                        this.getId(), expectedSequenceNo, sequenceNo);
                LOGGER.debug(logStr);
            }
            return true;
        }

        return false;
    }

    private void writeNoop(final HttpAcceptSession session) {
        String userAgent = session.getReadHeader("User-Agent");
        boolean isClientIE11 = false;
        if (userAgent != null && userAgent.contains("Trident/7.0")) {
            isClientIE11 = true;
        }

        // attach now or attach after commit if header flush is required
        if (!longpoll(session)) {
            // currently this is required for Silverlight as it seems to want some data to be
            // received before it will start to deliver messages
            // this is also needed to detect that streaming has initialized properly
            // so we don't fall back to encrypted streaming or long polling
            session.write(WsCommandMessage.NOOP);

            String flushDelay = session.getParameter(".kf");
            if (isClientIE11 && flushDelay == null) {
                flushDelay = "200";   //KG-10590 add .kf=200 for IE11 client
            }
            if (flushDelay != null) {
                final long flushDelayMillis = Integer.parseInt(flushDelay);
                // commit session and write out headers and any messages already in the queue
                CommitFuture commitFuture = session.commit();
                commitFuture.addListener(new IoFutureListener<CommitFuture>() {
                    @Override
                    public void operationComplete(CommitFuture future) {
                        // attach http session to wsf session
                        // after delay to force Silverlight client to notice payload
                        if (flushDelayMillis > 0L) {
                            Runnable command = new AttachParentCommand(WsebSession.this, session, flushDelayMillis);
                            scheduler.schedule(command, flushDelayMillis, TimeUnit.MILLISECONDS);
                        }
                    }
                });
            }
        }
    }


    private static boolean longpoll(HttpSession session) {
        return Long.valueOf(0L).equals(session.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY));
    }

    private class AttachParentCommand implements Runnable {

        private final WsebSession wsebSession;
        private final long flushDelayMillis;

        private AttachParentCommand(WsebSession wsebSession, HttpSession parent, long flushDelayMillis) {
            this.wsebSession = wsebSession;
            this.flushDelayMillis = flushDelayMillis;
        }

        @Override
        public void run() {
            // attaching the parent flushes buffered writes to HTTP response
            // but if connection has high latency, then intermediate TCP node
            // can cause server-delayed write to be combined into the same TCP packet
            // defeating the purpose of the delay (needed by Silverlight)
            // therefore, write a comment frame a little later as a backup to make
            // sure that the connection does not get stalled

            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 2, TimeUnit.MILLISECONDS);
            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 4, TimeUnit.MILLISECONDS);
            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 8, TimeUnit.MILLISECONDS);
        }
    }

    private class FlushCommand implements Runnable {

        private final WsebSession session;

        public FlushCommand(WsebSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            IoSession parent = session.getParent();
            if (parent != null && !parent.isClosing()) {
                parent.write(WsCommandMessage.NOOP);
            }
        }

    }

    // When sequence no are not used (for e.g old clients) and
    // If the first write request is long-polling, then it is out of order
    private boolean isLongPollingOutOfOrder(HttpAcceptSession session) {
        if (firstWriter && !validateSequenceNo && longpoll(session)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Closing HTTP session [HTTP#%d], WSEB session [WSEB#%d] as " +
                        "long-polling request is out of order", session.getId(), this.getId()));
            }
            return true;
        }
        return false;
    }

    private void closeSession(HttpAcceptSession session) {
        session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
        session.close(false);
        this.reset(new Exception("Out of order HTTP requests in WSEB").fillInStackTrace());
    }

    long nextReaderSequenceNo() {
        return readerSequenceNo;
    }

    long nextWriterSequenceNo() {
        return writerSequenceNo;
    }

    private final static BridgeAcceptProcessor<WsebSession> wsebSessionProcessor = new WsebSessionProcessor();

    private final static class WsebSessionProcessor extends BridgeAcceptProcessor<WsebSession> {

        @Override
        protected void removeInternal(WsebSession session) {
            session.getTransportSession().close(false);
        }

        @Override
        protected void flushInternal(final WsebSession session) {
            // get parent and check if null (no attached http session)
            final HttpSession writer = session.getWriter();
            if ( session.getService().getClass() == WsebAcceptor.class // TODO: make this neater
                    && (writer == null || writer.isClosing()) ) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("wsebSessionProcessor.flushInternal: returning because writer (%s) " +
                                                       "is null or writer is closing(%s)",
                            writer, writer==null ? "n/a" : Boolean.valueOf(writer.isClosing()) ));
                }
                return;
            }

            final IoSessionEx transport = session.getTransportSession();
            if (transport.isClosing()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("wsebSessionProcessor.flushInternal: returning because transport (%s) " +
                                                       "is closing", transport));
                }
                return;
            }

            IoFilterChain filterChain = session.getFilterChain();

            // get write request queue and process it
            final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
            WriteFuture lastWrite = null;
            do {
                WriteRequest request =  writeRequestQueue.poll(session);
                if (request == null) {
                    if (lastWrite == null) {
                        // queue was empty, make sure WsebAcceptProcessor / WsebConnectProcessor flush is called
                        // to handle padding for initial downstream response
                        ((AbstractIoSessionEx) transport).getProcessor().flush(transport);
                    }
                    break;
                }

                // identity compare for our marker as a command to reconnect the
                // stream
                if (WsebSession.isReconnectRequest(request)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(format("RECONNECT_REQUEST detected on wseb session %d: passing to wseb processor",
                                session.getId()));
                    }
                    // Bypass the filter chain since WebSocket extension filters should not see this special request.
                    // WsebAccept(or Connect)Processor will deal with it.
                    transport.getWriteRequestQueue().offer(transport, request);
                    if (!transport.isWriteSuspended()) {
                        ((AbstractIoSessionEx) transport).getProcessor().flush(transport);
                    }
                    continue;
                }

                // get message and compare to types we can process
                Object message = request.getMessage();
                if (message instanceof IoBufferEx) {
                    IoBufferEx buf = (IoBufferEx) message;
                    try {
                        // stop if parent already closing
                        if (transport.isClosing()) {
                            session.setCurrentWriteRequest(request);
                            break;
                        }

                        // hold current remaining bytes so we know how much was
                        // written
                        int remaining = buf.remaining();

                        if (remaining == 0) {
                            throw new IllegalStateException("Unexpected empty buffer");
                        }

                        // convert from session+buffer to message
                        if (buf instanceof WsBuffer) {
                            // reuse previously constructed message if available
                            WsBuffer wsBuffer = (WsBuffer)buf;
                            WsMessage wsebMessage = wsBuffer.getMessage();
                            if (wsebMessage == null) {
                                WsMessage newWsebMessage;
                                if (wsBuffer.getKind() == WsBuffer.Kind.TEXT) {
                                    //if the connection is mixed transport, send textmessage
                                    newWsebMessage = new WsTextMessage(buf);
                                }
                                else {
                                    newWsebMessage = new WsBinaryMessage(buf);
                                }

                                if (wsBuffer.isAutoCache()) {
                                    // buffer is cached on parent, continue with derived caching
                                    newWsebMessage.initCache();
                                }
                                boolean wasUpdated = wsBuffer.setMessage(newWsebMessage);
                                wsebMessage = wasUpdated ? newWsebMessage : wsBuffer.getMessage();
                            }
                            // flush the buffer out to the session
                            lastWrite = flushNowInternal(transport, wsebMessage, wsBuffer, filterChain, request);
                        }
                        else {
                            // flush the buffer out to the session
                            lastWrite = flushNowInternal(transport, new WsBinaryMessage(buf), buf, filterChain, request);
                        }
                    }
                    catch (Exception e) {
                        request.getFuture().setException(e);
                    }
                }
                else {
                    throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                }
            }
            while (true);
        }
    };

    private static final IoHandlerAdapter<TransportSession> transportHandler  = new TransportHandler();

    private static class TransportHandler extends IoHandlerAdapter<TransportSession> {


        @Override
        protected void doMessageReceived(TransportSession session, Object message) throws Exception {
            // this can happen if there is an error
            if (!(message instanceof WsMessage)) {
                return;
            }

            WsebSession wsebSession = session.getWsebSession();
            WsMessage wsMessage = (WsMessage)message;
            IoBufferEx data = wsMessage.getBytes();

            switch (wsMessage.getKind()) {
            case BINARY:
                IoFilterChain filterChain = wsebSession.getFilterChain();
                WsebBufferAllocator allocator = (WsebBufferAllocator) wsebSession.getBufferAllocator();
                WsBuffer wsBinaryBuffer = allocator.wrap(data.buf());
                filterChain.fireMessageReceived(wsBinaryBuffer);
                break;
            case TEXT:
                filterChain = wsebSession.getFilterChain();
                allocator = (WsebBufferAllocator) wsebSession.getBufferAllocator();
                WsBuffer wsTextBuffer = allocator.wrap(data.buf());
                wsTextBuffer.setKind(WsBuffer.Kind.TEXT);
                filterChain.fireMessageReceived(wsTextBuffer);
                break;
            case PING:
                allocator = (WsebBufferAllocator) wsebSession.getBufferAllocator();
                IoBufferEx emptyBuf = allocator.wrap(allocator.allocate(0));
                emptyBuf.mark();
                WsMessage emptyPong = new WsPongMessage(emptyBuf);
                session.write(emptyPong);
                break;
            case CLOSE:
                wsebSession.close(false);
                break;
            case CONTINUATION:
                break;
            case PONG:
                break;
            case COMMAND:
                break;
            default:
                break;
            }
        }

        @Override
        protected void doExceptionCaught(TransportSession session, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Exception while handling upstream WebSocket frame for WsebSession: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }

            session.close(true);
        }

        @Override
        protected void doSessionClosed(TransportSession session) throws Exception {
            WsebSession wsebSession = session.getWsebSession();
            if (wsebSession != null && !wsebSession.isClosing()) {
                wsebSession.reset(new Exception("Network connectivity has been lost or transport was closed at other end").fillInStackTrace());
            }
        }

    };

    /**
     * This processor, set on the TransportSession, just delegates to WsebAcceptProcessor or WsebConnectProcessor.
     * We cannot set those directly as the processor on the TransportSession because of parameterized type mismatches.
     */
    static class TransportProcessor implements IoProcessorEx<TransportSession> {
        private final IoProcessorEx<WsebSession> processor;

        TransportProcessor(IoProcessorEx<WsebSession> processor) {
            this.processor = processor;
        }

        @Override
        public boolean isDisposing() {
            return processor.isDisposing();
        }

        @Override
        public boolean isDisposed() {
            return processor.isDisposed();
        }

        @Override
        public void dispose() {
            processor.dispose ();
        }

        @Override
        public void add(TransportSession session) {
            // will never be called
        }

        @Override
        public void flush(TransportSession session) {
            processor.flush(session.getWsebSession());

        }

        @Override
        public void updateTrafficControl(TransportSession session) {
            processor.updateTrafficControl(session.getWsebSession());

        }

        @Override
        public void remove(TransportSession session) {
            processor.remove(session.getWsebSession());
        }
    }

    static class TransportSession extends DummySessionEx {
        private final WsebSession wsebSession;

        TransportSession(WsebSession wsebSession, IoProcessorEx<WsebSession> processor) {
            super(wsebSession.getIoThread(), wsebSession.getIoExecutor(), new TransportProcessor(processor));
            this.wsebSession = wsebSession;
        }

        @Override
        public IoBufferAllocatorEx<?> getBufferAllocator() {
            return wsebSession.getBufferAllocator();
        }

        WsebSession getWsebSession() {
            return wsebSession;
        }
    }

}
