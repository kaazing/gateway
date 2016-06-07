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
package org.kaazing.gateway.transport.wseb;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.wseb.WsebDownstreamHandler.TIME_TO_TIMEOUT_RECONNECT_MILLIS;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.gateway.util.InternalSystemProperty.WS_CLOSE_TIMEOUT;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.DummySessionEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;

/**
 * Explanation of TransportSession, etc
 *
 * TransportSession in an IoSession encapsulated within the WsebSession which is used to expose all incoming
 * and outgoing WS frames to WebSocket extension filters. It is also used for inactivity timeout (WsCheckAliveFilter
 * is added to its filter chain). Incoming events (like messageReceived, sessionClosed) transit as follows:<p>
 *
 * WsebUpstreamHandler -> TransportSession's filter chain -> TransportHandler -> WsebSession's filter chain -> service handler
 *
 * <p>Outgoing events (like filterWrite, filterClose) flow as follows:<br><br>
 *
 * WsebSession's filter chain -> WsebSessionProcessor -> TransportSessions' filter chain -> TransportProcessor
 * -> WsebAcceptProcessor -> downstream http session
 *
 */
public class WsebSession extends AbstractWsBridgeSession<WsebSession, WsBuffer> {

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

    private final Logger logger;
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

    private final boolean specCompliant;

    private final Runnable enqueueReconnectAndFlushTask = new Runnable() {
        @Override public void run() {
            enqueueReconnectAndFlush0();
        }
    };
    private ScheduledFuture<?> timeoutFuture;

    private TransportSession transportSession;

    private enum CloseState {
        CLOSE_SENT,
        CLOSE_RECEIVED
    }

    private EnumSet<CloseState> closeState = EnumSet.noneOf(CloseState.class);
    private final long closeTimeout;
    private boolean pingEnabled = false;

    public WsebSession(int ioLayer,
                       Thread ioThread,
                       Executor ioExecutor,
                       IoServiceEx service,
                       IoProcessorEx<WsebSession> processor,
                       ResourceAddress localAddress,
                       ResourceAddress remoteAddress,
                       IoBufferAllocatorEx<WsBuffer> allocator,
                       ResultAwareLoginContext loginContext,
                       int clientIdleTimeout,
                       long inactivityTimeout,
                       boolean validateSequenceNo,
                       long sequenceNo,
                       List<WebSocketExtension> extensions,
                       Logger logger,
                       Properties configuration) {
        super(ioLayer,
              ioThread,
              ioExecutor,
              service,
              wsebSessionProcessor,
              localAddress,
              remoteAddress,
              allocator,
              Direction.BOTH,
              loginContext,
              extensions);
        this.logger = logger;
        attachingWrite = new AtomicBoolean(false);
        readSession = new AtomicReference<>();
        pendingNewWriter = new AtomicReference<>();
        timeout = new TimeoutCommand(this);
        this.clientIdleTimeout = clientIdleTimeout;
        this.inactivityTimeout = inactivityTimeout;
        this.validateSequenceNo = validateSequenceNo;
        readerSequenceNo = sequenceNo+1;
        writerSequenceNo = sequenceNo+1;
        specCompliant = WSE_SPECIFICATION.getBooleanProperty(configuration);
        transportSession = new TransportSession(this, processor);
        transportSession.setHandler(transportHandler);
        closeTimeout = Utils.parseTimeInterval(WS_CLOSE_TIMEOUT.getProperty(configuration), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void setIoAlignment0(Thread ioThread, Executor ioExecutor) {
        transportSession.setIoAlignment(ioThread, ioExecutor);
        IoSessionEx reader = getReader();
        if (reader != null) {
            reader.setIoAlignment(ioThread, ioExecutor);
        }
        // No need to align writer here as it gets re-aligned since it is parent session
        // should we detachReader()/detachWriter() too ?

        super.setIoAlignment0(ioThread, ioExecutor);
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
    }

    private void attachWriter0(final HttpSession newWriter) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("attachWriter on WsebSession wseb#%d, newWriter=%s", this.getId(), newWriter));
        }
        reconnecting.set(false);
        if (!getTransportSession().isClosing()) {
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
                    if (!checkLongPollingOrder(newAcceptWriter) || !checkWriterOrder(newAcceptWriter)) {
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
                if (!isCloseSent()) {
                    newWriter.write(WsCommandMessage.CLOSE);
                    newWriter.write(WsCommandMessage.RECONNECT);
                }
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
            final Executor ioExecutor = getIoExecutor();
            ioExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    detachWriter0(oldWriter);
                }
            });
        }

        return detached;
    }

    private void detachWriter0(final HttpSession oldWriter) {
        if (oldWriter.getIoThread() == getIoThread()) {
            if (!oldWriter.isClosing()) {
                oldWriter.write(WsCommandMessage.RECONNECT);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("detachWriter on WsebSession wseb#%d, oldWriter=%s", this.getId(), oldWriter));
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
            final Thread ioThread = getIoThread();
            final Executor ioExecutor = getIoExecutor();
            // Prevent messageReceived from being fired from setIoAlignment and racing ahead of attachReader0
            newReader.suspendRead();
            newReader.setIoAlignment(NO_THREAD, NO_EXECUTOR);
            ioExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    newReader.setIoAlignment(ioThread, ioExecutor);
                    attachReader0(newReader);
                    // now allow messageReceived to fire if data is available
                    newReader.resumeRead();
                }
            });
        }

    }

    private void attachReader0(final IoSessionEx newReader) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("attachReader on WsebSession wseb#%d, newReader=%s", this.getId(), newReader));
        }
        // TODO: needs improved handling of old value for overlapping downstream
        //       from client perspective to detect buffering proxies
        // TODO: needs re-alignment similar to attachWriter
        if (newReader instanceof HttpAcceptSession) {
            HttpAcceptSession newAcceptReader = (HttpAcceptSession) newReader;
            if (!checkReaderOrder(newAcceptReader)) {
                return;
            }
        }

        readerSequenceNo++;
        IoSessionEx oldReader = readSession.get();
        if (oldReader != null && !oldReader.isClosing() && oldReader instanceof HttpAcceptSession) {
            // Overlapping upstream, forbidden
            String message = "Overlapping upstream request";
            setCloseException(new IOException(message));
            HttpStatus status = HttpStatus.CLIENT_BAD_REQUEST;
            HttpAcceptSession newAcceptReader = (HttpAcceptSession) newReader;
            newAcceptReader.setStatus(status);
            newAcceptReader.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
            newAcceptReader.close(true);
            return;
        }
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
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("enqueueReconnectRequest on WsebSession %s", this));
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

    private boolean checkWriterOrder(HttpAcceptSession session) {
        if (validateSequenceNo) {
            return checkOrder(session, writerSequenceNo);
        }
        return true;
    }

    private boolean checkReaderOrder(HttpAcceptSession session) {
        if (validateSequenceNo) {
            return checkOrder(session, readerSequenceNo);
        }
        return true;
    }

    private boolean checkOrder(HttpAcceptSession session, long expectedSequenceNo) {
        String sequenceNo = session.getReadHeader(HttpHeaders.HEADER_X_SEQUENCE_NO);

        if (sequenceNo == null || expectedSequenceNo != Long.parseLong(sequenceNo)) {
            String message = String.format("Out of order request for session=%s: expected seq no=%d, got=%s",
                    session, expectedSequenceNo, sequenceNo);
            setCloseException(new IOException(message));
            HttpStatus status = HttpStatus.CLIENT_BAD_REQUEST;
            session.setStatus(status);
            session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
            session.close(true);
            return false;
        }

        return true;
    }

    boolean isCloseReceived() {
        return closeState.contains(CloseState.CLOSE_RECEIVED);
    }

    private void setCloseReceived() {
        closeState.add(CloseState.CLOSE_RECEIVED);
    }

    boolean isCloseSent() {
        return closeState.contains(CloseState.CLOSE_SENT);
    }

    private void setCloseSent() {
        closeState.add(CloseState.CLOSE_SENT);
    }

    Logger getLogger() {
        return logger;
    }

    boolean isPingEnabled() {
        return pingEnabled;
    }

    void setPingEnabled(boolean enabled) {
        pingEnabled = enabled;
    }

    private void writeNoop(final HttpAcceptSession session) {
        String userAgent = session.getReadHeader("User-Agent");
        boolean isClientIE11 = false;
        if (userAgent != null && userAgent.contains("Trident/7.0")) {
            isClientIE11 = true;
        }

        // attach now or attach after commit if header flush is required
        if (!longpoll(session)) {

            if (specCompliant) {
                // Conform to WSE specification. Do not write NOOP unless specifically requested by query parameters.
                // Do not commit if this a WsebConnector session because that prevents the request headers from being
                // written
                // in HttpConnectProcessor.flushInternal().
                if (session.getService() instanceof HttpAcceptor) {
                    session.commit();
                }
            } else {
                // To be safe do as we did before (in 3.5, 4.0): always write NOOP at start of downstream
                session.write(WsCommandMessage.NOOP);
            }

            String flushDelay = session.getParameter(".kf");
            if (isClientIE11 && flushDelay == null) {
                flushDelay = "200";   //KG-10590 add .kf=200 for IE11 client
            }
            if (flushDelay != null) {

                if (specCompliant) {
                    // currently this is required for Silverlight as it seems to want some data to be
                    // received before it will start to deliver messages
                    // this is also needed to detect that streaming has initialized properly
                    // so we don't fall back to encrypted streaming or long polling
                    session.write(WsCommandMessage.NOOP);
                }

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

    private static final BridgeAcceptProcessor<WsebSession> wsebSessionProcessor = new WsebSessionProcessor();

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
    private boolean checkLongPollingOrder(HttpAcceptSession session) {
        if (firstWriter && !validateSequenceNo && longpoll(session)) {
            String message = "Out of order long-polling request, must not be first";
            setCloseException(new IOException(message));
            HttpStatus status = HttpStatus.CLIENT_BAD_REQUEST;
            session.setStatus(status);
            session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
            session.close(true);
            return false;
        }
        return true;
    }

    long nextReaderSequenceNo() {
        return readerSequenceNo;
    }

    long nextWriterSequenceNo() {
        return writerSequenceNo;
    }

    private static final class WsebSessionProcessor extends BridgeAcceptProcessor<WsebSession> {

        @Override
        protected void removeInternal(WsebSession session) {
            if (cannotWrite(session)) {
                // can't do close handshake
                session.getTransportSession().close(true);
                return;
            }
            WsCloseMessage closeMessage = WsCloseMessage.NORMAL_CLOSE;
            if (session.getLogger().isDebugEnabled()) {
                session.getLogger().debug(String.format("Writing WS CLOSE frame to transport of wseb session %s", session));
            }
            // Write may not be immediate, especially for WsebConnector case where writer is not immediately attached
            session.getTransportSession().write(closeMessage).addListener(new IoFutureListener<WriteFuture>() {

                @Override
                public void operationComplete(WriteFuture future) {
                    if (!future.isWritten()) {
                        // can't do close handshake
                        session.getTransportSession().close(true);
                    }
                    else if (session.isCloseReceived()) {
                        session.getTransportSession().close(true);
                    }
                    else {
                        // Wait for close handshake completion from client on upstream
                        session.setCloseSent();
                        session.getTransportSession().getConfig().setIdleTimeInMillis(
                                IdleStatus.READER_IDLE, session.closeTimeout);
                    }
                }

            });

        }

        @Override
        protected void doFireSessionDestroyed(WsebSession session) {
            // We must fire session destroyed on the wsebSession only when the close handshake is complete
            // (which is when the transport session gets closed). This is done in
            // WsebAccept(Connect)Processor.remove (called from TransportSessionProcessor.remove)
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void flushInternal(final WsebSession session) {
            if (cannotWrite(session)) {
                if (session.getLogger().isTraceEnabled()) {
                    session.getLogger().trace(String.format("wsebSessionProcessor.flushInternal: returning because writer (%s) " +
                                                       "is null or writer is closing",
                            session.getWriter()));
                }
                return;
            }

            final IoSessionEx transport = session.getTransportSession();
            if (transport.isClosing()) {
                if (session.getLogger().isTraceEnabled()) {
                    session.getLogger().trace(String.format("wsebSessionProcessor.flushInternal: returning because transport (%s) " +
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
                    if (session.getLogger().isDebugEnabled()) {
                        session.getLogger().debug(format("RECONNECT_REQUEST detected on wseb session %d: passing to wseb processor",
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

        private boolean cannotWrite(WsebSession session) {
            // get parent and check if null (no attached http session)
            boolean isAcceptor = session.getService().getClass() == WsebAcceptor.class; // TODO: make this neater
            final HttpSession writer = session.getWriter();
            if (isAcceptor) {
                if (writer == null || writer.isClosing()) {
                    return true;
                }
            }
            else if (session.getWriteAddress() == null) {
                // WsebConnector create handshake failed
                return true;
            }
            return false;
        }
    }

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
                wsebSession.setCloseReceived();
                if (!wsebSession.isClosing()) {
                    wsebSession.close(false);
                }
                else {
                    // No need flush, we know WS_CLOSE frame has already been written by WsebSessionProcessor.removeInternal
                    wsebSession.getTransportSession().close(true);
                }
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
            if (session.getLogger().isDebugEnabled()) {
                String message = format("Exception while handling upstream WebSocket frame for WsebSession: %s", cause);
                if (session.getLogger().isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    session.getLogger().debug(message, cause);
                }
                else {
                    session.getLogger().debug(message);
                }
            }

            session.close(true);
        }

        @Override
        protected void doSessionIdle(TransportSession session, IdleStatus status) throws Exception {
            WsebSession wsebSession = session.getWsebSession();
            if (wsebSession.isCloseSent() && !wsebSession.isCloseReceived()) {
                if (session.getLogger().isDebugEnabled()) {
                    session.getLogger().debug(format("Close handshake timeout while closing wseb session %s", session));
                }
                session.close(true);
            }
        }

    }

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

        Logger getLogger() {
            return wsebSession.getLogger();
        }

        @Override
        public String toString() {
            return String.format("[wseb#%s transport]", wsebSession.getId());
        }
    }

}
