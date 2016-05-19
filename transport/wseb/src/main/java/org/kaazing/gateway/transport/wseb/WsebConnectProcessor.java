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

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_TRANSFER_ENCODING;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_SEQUENCE_NO;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeConnectProcessor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpConnectProcessor;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.gateway.transport.wseb.filter.WsebFrameCodecFilter;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.slf4j.Logger;

@SuppressWarnings("deprecation")
class WsebConnectProcessor extends BridgeConnectProcessor<WsebSession> {
    private final Logger logger;

    private final boolean specCompliant;

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";

    private final WsebFrameCodecFilter wsebFraming = new WsebFrameCodecFilter(0, true);
    private final BridgeServiceFactory bridgeServiceFactory;

    public WsebConnectProcessor(BridgeServiceFactory bridgeServiceFactory, Logger logger, boolean specCompliant) {
        super();
        this.bridgeServiceFactory = bridgeServiceFactory;
        this.logger = logger;
        this.specCompliant = specCompliant;
    }

    @Override
    protected void removeInternal(WsebSession session) {
        HttpSession writer = session.getWriter();
        if (writer != null) {
            finishWrite(session, writer);
        }
    }

    @Override
    protected void flushInternal(final WsebSession session) {
        IoSessionEx transportSession = session.getTransportSession();
        IoFilterChain filterChain = transportSession.getFilterChain();

        // get parent and check if null (no attached http session)
        final HttpConnectSession writer = (HttpConnectSession)session.getWriter();

        // store last write so we can observe it
        WriteFuture lastWrite;

        // TODO: thread safety
    	// multiple threads can trigger a reconnect on the same WsfSession
        final AtomicBoolean reconnecting = new AtomicBoolean(false);

        // we can still have a current write request during the transition between writers
        WriteRequest currentWriteRequest = session.getCurrentWriteRequest();
        if (currentWriteRequest != null) {
            session.setCurrentWriteRequest(null);
        }

        // get write request queue and process it
        final WriteRequestQueue writeRequestQueue = transportSession.getWriteRequestQueue();
        do {
            // get current request in the event that it was not complete last
            // iteration
            WriteRequest request = currentWriteRequest;

            // if we have no more requests then we are done flushing the queue
            if (request == null) {
                // if request is null get next one off the queue
                request = writeRequestQueue.poll(transportSession);
                if (request == null) {
                    break;
                }
            }
            else {
                currentWriteRequest = null;
            }

            // identity compare for our marker as a command to reconnect the
            // stream
            if (WsebSession.isReconnectRequest(request)) {
                if (writer == null) {
                    session.setCurrentWriteRequest(request);
                    initWriter(session);
                    break;
                }

                assert (!writer.isWriteSuspended());
                writer.write(WsCommandMessage.RECONNECT);
                finishWrite(session, writer);
                break;
            }

            // get message and compare to types we can process
            Object message = request.getMessage();
            if (message instanceof WsMessage) {
                WsMessage frame = (WsMessage) message;
                IoBufferEx buffer = frame.getBytes();

                try {
                    if (writer == null) {
                        session.setCurrentWriteRequest(request);
                        initWriter(session);
                        break;
                    }

                    // stop if parent already closing
                    if (writer.isClosing()) {
                        request.getFuture().setException(new IOException("Writer is closing"));
                        break;
                    }

                    if (frame.getKind() == Kind.CLOSE) {
                        writer.write(WsCommandMessage.CLOSE);
                        // Detach writer to send RECONNECT and because no more data can now be written to the client.
                        session.detachWriter(writer);
                        request.getFuture().setWritten();
                        break;
                    }

                    // hold current remaining bytes so we know how much was
                    // written
                    int remaining = buffer.remaining();

                    // flush the buffer out to the session
                    lastWrite = flushNowInternal(writer, frame, buffer, filterChain, request);

                    // increment session written bytes
                    int written = remaining;
                    session.increaseWrittenBytes(written, System.currentTimeMillis());

                    // if we are not already reconnecting then add a listener to
                    // the last write future
                    // so it can check the written bytes and compare to client
                    // buffer
                    if (reconnecting.get() == false) {
                        lastWrite.addListener(new CheckBuffer(session, reconnecting));
                    }
                }
                catch (Exception e) {
                    request.getFuture().setException(e);
                }
            }
            else
            {
                throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
            }
        }
        while (true);
    }

    private void finishWrite(final WsebSession session, final HttpSession writer) {
        // ensure upstream is serialized to avoid out-of-order delivery
        session.suspendWrite();

        // writer.write(IoBuffer.allocate(0)) throws exception
        // but we need to signal to HTTP layer that write is complete
        WriteFutureEx writeFuture = new DefaultWriteFutureEx(writer);
        WriteRequestEx writeRequest = new DefaultWriteRequestEx(HttpConnectProcessor.WRITE_COMPLETE, writeFuture);
        IoFilterChain filterChain = writer.getFilterChain();
        filterChain.fireFilterWrite(writeRequest);

        // wait for writer to close before detaching the writer and flushing
        writer.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                session.detachWriter(writer);

                //TODO: remove suspendWrite/resumeWrite completely (replace with a WSEB-specific "send queue")
                session.resumeWrite();
                // We are always aligned now. if (session.isIoAligned()) {;
                session.getProcessor().flush(session);
            }
        });
    }


    private void initWriter(final WsebSession session) {
        final ResourceAddress writeAddress = session.getWriteAddress();
        if (writeAddress == null) {     // create sessionClosed not yet completed
            return;
        }
        if (session.compareAndSetAttachingWrite(false, true)) {
            BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(writeAddress);
            ConnectFuture connectFuture =
                    connector.connect(writeAddress, new WriteHandler(session),
                            selectTransportSessionInitializer(session, writeAddress)
                    );

            final IoFutureListener<ConnectFuture> connectFutureIoFutureListener =
                    selectConnectFutureListener(session, writeAddress);
            connectFuture.addListener(connectFutureIoFutureListener);
        }
    }

    public IoSessionInitializer<ConnectFuture> selectTransportSessionInitializer(final WsebSession wsebSession, ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return new IoSessionInitializer<ConnectFuture>() {
                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    HttpConnectSession writeSession = (HttpConnectSession) session;
                    writeSession.setMethod(HttpMethod.POST);
                    writeSession.setWriteHeader(HEADER_TRANSFER_ENCODING, "chunked");
                    writeSession.setWriteHeader(HEADER_X_SEQUENCE_NO, Long.toString(wsebSession.nextWriterSequenceNo()));

                    // Avoid default to httpxe for efficiency (single http transport layer at other end)
                    // and to allowed chunking to work (does not currently work with httpxe)
                    writeSession.setWriteHeader(HttpHeaders.HEADER_X_NEXT_PROTOCOL, "wse/1.0");

                    if (specCompliant) {
                        // WSE specification requires Content-type header on upstream requests
                        writeSession.setWriteHeader(HEADER_CONTENT_TYPE, "application/octet-stream");
                    }

                    // Note: deferring this to writeHandler.sessionOpened creates a race condition
                    IoFilterChain filterChain = writeSession.getFilterChain();
                    filterChain.addLast(CODEC_FILTER, wsebFraming);
                }
            };
        }
        throw new RuntimeException("No session initializer available for address "+address);
    }

    private IoFutureListener<ConnectFuture> selectConnectFutureListener(final WsebSession session, ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    // attaching the write auto-flushes the processor
                    try {
                        HttpSession writeSession = (HttpSession) future.getSession();
                        session.attachWriter(writeSession);
                        // implicit call to flush may be gated by semaphore
                        // so force call to flushInternal directly instead
                        flushInternal(session);
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Caught exception {} on session {} while attaching writer or flushing", e,
                                    session);
                            if (logger.isTraceEnabled()) {
                                logger.trace("Exception stack trace: ", e);
                            }
                        }
                        session.close(true);
                    }
                }
            };
        }
        throw new RuntimeException("No connect listener available for address "+address);
    }

    private static final class CheckBuffer implements IoFutureListener<WriteFuture> {

        private final WsebSession wsebSession;
        private final AtomicBoolean reconnecting;

        public CheckBuffer(WsebSession wsebSession, AtomicBoolean reconnecting) {
            this.wsebSession = wsebSession;
            this.reconnecting = reconnecting;
        }

        @Override
        public void operationComplete(WriteFuture future) {
            HttpConnectSession parent = (HttpConnectSession)future.getSession();
            if (parent.isClosing() || reconnecting.get() == true) {
                return;
            }
            // Note: for now we always have a client buffer low water mark of 0
            //       to trigger "long-polling" for upstream
            // check to see if we have written out at least enough bytes to be
            // over the client buffer
            Long clientBuffer = (Long)parent.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY, Long.MAX_VALUE);  // TODO: sync with precise Content-Length instead
            if (clientBuffer != null) {
                long bytesWritten = parent.getWrittenBytes();
                if (bytesWritten >= clientBuffer) {
                    // TODO: thread safety
                    // multiple threads can trigger a reconnect on the same WsfSession
                	if (reconnecting.compareAndSet(false, true)) {
	                    wsebSession.enqueueReconnectRequest();
                	}
                }
            }
        }
    }

    private final class WriteHandler extends IoHandlerAdapter<HttpSession> {
        private final WsebSession wsebSession;

        WriteHandler(WsebSession wsebSession) {
            this.wsebSession = wsebSession;
        }

        @Override
        protected void doExceptionCaught(HttpSession session, Throwable cause) throws Exception {
            wsebSession.setCloseException(cause);
            session.close(true);
        }

        @Override
        protected void doSessionClosed(HttpSession session) throws Exception {
            if (session.getStatus() != HttpStatus.SUCCESS_OK || wsebSession.getCloseException() != null) {
                wsebSession.reset(
                        new IOException("Network connectivity has been lost or transport was closed at other end",
                                wsebSession.getAndClearCloseException()).fillInStackTrace());
            }
        }
    }
}
