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
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;

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
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wseb.filter.WsebFrameCodecFilter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsebConnectProcessor extends BridgeConnectProcessor<WsebSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WsebConnectProcessor.class);

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";
    protected static final WriteRequest CLOSE_REQUEST = new DefaultWriteRequestEx(new Object());

    private final WsebFrameCodecFilter wsebFraming = new WsebFrameCodecFilter(0);
    private final BridgeServiceFactory bridgeServiceFactory;

    public WsebConnectProcessor(BridgeServiceFactory bridgeServiceFactory) {
        super();
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Override
    protected void removeInternal(WsebSession session) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        writeRequestQueue.offer(session, CLOSE_REQUEST);
        flushInternal(session);
    }

    @Override
    protected void flushInternal(final WsebSession session) {
        IoFilterChain filterChain = session.getTransportSession().getFilterChain();

        // get parent and check if null (no attached http session)
        final HttpConnectSession writer = (HttpConnectSession)session.getWriter();

        // store last write so we can observe it
        WriteFuture lastWrite = null;

        // TODO: thread safety
    	// multiple threads can trigger a reconnect on the same WsfSession
        final AtomicBoolean reconnecting = new AtomicBoolean(false);

        // we can still have a current write request during the transition between writers
        WriteRequest currentWriteRequest = session.getCurrentWriteRequest();
        if (currentWriteRequest != null) {
            session.setCurrentWriteRequest(null);
        }
        
        // get write request queue and process it
        final WriteRequestQueue writeRequestQueue = session.getTransportSession().getWriteRequestQueue();
        do {
            // get current request in the event that it was not complete last
            // iteration
            WriteRequest request = currentWriteRequest;

            // if we have no more requests then we are done flushing the queue
            if (request == null) {
                // if request is null get next one off the queue
                request = writeRequestQueue.poll(session);
                if (request == null) {
                    break;
                }
            }
            else {
                currentWriteRequest = null;
            }

            if (request == CLOSE_REQUEST) {
                // TODO: resolve infinite loop problem
                if (session.isClosing()) {
                    break;
                }

                if (writer == null) {
                    session.setCurrentWriteRequest(request);
                    initWriter(session);
                    break;
                }

                assert (writer != null);
                assert (!writer.isWriteSuspended());
                writer.write(WsCommandMessage.CLOSE);
                finishWrite(session, writer);
                break;
            }

            // identity compare for our marker as a command to reconnect the
            // stream
            if (WsebSession.isReconnectRequest(request)) {
                if (writer == null) {
                    session.setCurrentWriteRequest(request);
                    initWriter(session);
                    break;
                }

                assert (writer != null);
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
                    // hold current remaining bytes so we know how much was
                    // written
                    int remaining = buffer.remaining();

                    if (writer == null) {
                        session.setCurrentWriteRequest(request);
                        initWriter(session);
                        break;
                    }

                    assert (writer != null);

                    // stop if parent already closing
                    if (writer.isClosing()) {
                        break;
                    }

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
        if (session.compareAndSetAttachingWrite(false, true)) {


            final ResourceAddress writeAddress = session.getWriteAddress();
            BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(writeAddress);
            ConnectFuture connectFuture =
                    connector.connect(writeAddress, writeHandler,
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
                    writeSession.setWriteHeader(HEADER_CONTENT_LENGTH, Long.toString(Long.MAX_VALUE));
                    writeSession.setWriteHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, Long.toString(wsebSession.nextWriterSequenceNo()));

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

    private final IoHandlerAdapter<HttpSession> writeHandler = new IoHandlerAdapter<HttpSession>() {
        @Override
        protected void doExceptionCaught(HttpSession session, Throwable cause) throws Exception {
            // Does not appear to be an easy way to access the wsebSession and it might not be the upstream's
            // job anyhow.  If we get an exception fired here we will log one line unless trace is enabled.
            if (LOGGER.isDebugEnabled()) {
                String message = format("Exception while handling HTTP upstream for WseConnectProcessor: %s", cause);
                if (LOGGER.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    LOGGER.debug(message, cause);
                }
                else {
                    LOGGER.debug(message);
                }
            }
            session.close(true);
        }
    };
}
