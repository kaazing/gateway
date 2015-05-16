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

import org.kaazing.gateway.transport.http.HttpSession;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wseb.filter.WsebFrameEncoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;


public class WsebAcceptProcessor extends BridgeAcceptProcessor<WsebSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WsebAcceptProcessor.class);
    private static final CheckInitialPadding CHECK_INITIAL_PADDING = new CheckInitialPadding();
    private final ScheduledExecutorService scheduler;

    public WsebAcceptProcessor(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    protected void removeInternal(WsebSession session) {
        HttpSession writer = session.getWriter();
        if (writer != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("CLOSE command is written to writer %d", writer.getId()));
            }
            writer.write(WsCommandMessage.CLOSE);
            session.detachWriter(writer);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("NOT sending CLOSE command for session = %s as there is no attached writer", session));
            }
        }

        // Shouldn't we detach any pending writer ?
        // session.detachPendingWriter();
        session.cancelTimeout();
    }

    @Override
    protected void flushInternal(final WsebSession session) {
        // get parent and check if null (no attached http session)
        final HttpAcceptSession writer = (HttpAcceptSession)session.getWriter();
        if (writer == null || writer.isClosing()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("WsebAcceptProcessor.flushInternal: returning because writer (%s) " +
                                                   "is null or writer is closing(%s)",
                        writer, writer==null ? "n/a" : Boolean.valueOf(writer.isClosing()) ));
            }
            return;
        }

        // store last write so we can observe it
        WriteFuture lastWrite = null;

        IoFilterChain filterChain = session.getFilterChain();

        // we can still have a current write request during the transition between writers
        WriteRequest currentWriteRequest = session.getCurrentWriteRequest();
        if (currentWriteRequest != null) {
            session.setCurrentWriteRequest(null);
        }

        // get write request queue and process it
        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        Long clientBuffer = (Long) writer.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY);
        do {
            // get current request in the event that it was not complete last
            // iteration
            WriteRequest request = currentWriteRequest;

            // if we have no more requests then we are done flushing the queue
            if (request == null) {
                // if request is null get next one off the queue
                request = writeRequestQueue.poll(session);
                if (request == null) {
                    // closing so no need to calculate padding
                    if (session.isClosing() || writer.isClosing()) {
                        break;
                    }
                    // check if we wrote something this flush, if so add padding
                    // check
                    if (lastWrite != null) {
                        lastWrite.addListener(CHECK_INITIAL_PADDING);
                    }
                    else {
                        // nothing was in the queue to write at all so check if we
                        // should send padding preemptively
                        checkInitialPadding(writer);
                    }
                    // See if the http layer (for e.g. revalidate) data needs to be flushed
                    checkBuffer(writer, session);
                    break;
                }
            }
            else {
                currentWriteRequest = null;
            }

            // identity compare for our marker as a command to reconnect the
            // stream
            if (WsebSession.isReconnectRequest(request)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("RECONNECT_REQUEST detected: closing writer %d", writer.getId()));
                }
                // detaching the writer nulls the parent reference
                    session.detachWriter(writer);
                boolean attached = session.attachPendingWriter();
                if (!attached) {
                    session.scheduleTimeout(scheduler);
                }
                break;
            }

            // get message and compare to types we can process
            Object message = request.getMessage();
            if (message instanceof IoBufferEx) {
                IoBufferEx buf = (IoBufferEx) message;
                try {
                    // stop if parent already closing
                    if (writer.isClosing()) {
                        session.setCurrentWriteRequest(request);
                        break;
                    }

                    // hold current remaining bytes so we know how much was
                    // written
                    int remaining = buf.remaining();

                    if (remaining == 0) {
                        throw new IllegalStateException("Unexpected empty buffer");
                    }


                    // TODO: thread safety
                    // reconnect parent.close(false) above triggers flush of pending
                    // writes before closing the HTTP session, and in the interim
                    // parent.isClosing() returns false until the close begins
                    // since this flush method is gated by parent being null
                    // or closing, there is a race condition that would permit
                    // writing data to the parent during this interim state
                    // resulting in a WriteToClosedSessionException and losing data

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
                        lastWrite = flushNowInternal(writer, wsebMessage, wsBuffer, filterChain, request);
                    }
                    else {
                        // flush the buffer out to the session
                        lastWrite = flushNowInternal(writer, new WsBinaryMessage(buf), buf, filterChain, request);
                    }

                    // increment session written bytes
                    int written = remaining;
                    session.increaseWrittenBytes(written, System.currentTimeMillis());

                    // if we are not already reconnecting then add a listener to
                    // the last write future
                    // so it can check the written bytes and compare to client
                    // buffer
                    if (!session.isReconnecting()) {
                        // Check whether we require block padding
                        boolean checkBlockPadding = (writer.getAttribute(WsebAcceptor.CLIENT_BLOCK_PADDING_KEY) != null);
                        if (checkBlockPadding) {
                            checkBufferPadding(writer, session);
                        }
                        else {
                            // Don't incur overhead of the write future if there was no .kb parameter
                            if (clientBuffer != null) {
                                checkBuffer(writer, session);
                            }
                        }
                    }

                    // wrote the last bytes of the message to link the last
                    // write close to message close
                }
                catch (Exception e) {
                    request.getFuture().setException(e);
                }
            }
            else if (WsebSession.isPingRequest(request) || WsebSession.isPongRequest(request)) {
                boolean ping = WsebSession.isPingRequest(request);
                if (LOGGER.isDebugEnabled()) {
                    String poing = ping ? "PING" : "PONG";
                    LOGGER.debug(String.format("%s_REQUEST detected on wsebSession %s: sending %s",
                            poing, session, poing));
                }
                try {
                    // stop if parent already closing
                    if (writer.isClosing()) {
                        break;
                    }
                    IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                    IoBufferEx emptyBuf = allocator.wrap(allocator.allocate(0));
                    emptyBuf.mark();
                    WsMessage emptyPoing = ping ? new WsPingMessage(emptyBuf) : new WsPongMessage(emptyBuf);
                    // The following causes ClassCastException in stomp decoder in messageSent in JMS edition (see KG-9329)
                    // flushNowInternal(writer, emptyPoing, emptyBuf, filterChain, request);
                    writer.write(emptyPoing);
                }
                finally {
                    session.setCurrentWriteRequest(null);
                }
            }
            else {
                throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
            }
        }
        while (true);
    }

    private static void checkInitialPadding(HttpAcceptSession session) {
        // check to see if we need to add a padding message to the end of
        // the sent messages
        Integer clientPadding = (Integer)session.getAttribute(WsebAcceptor.CLIENT_PADDING_KEY);
        if (clientPadding != null) {
            long writtenBytes = session.getWrittenBytes();
            int padding = (int)(clientPadding - writtenBytes);
            if (padding > 0) {
                // each command uses 2 bytes on the wire so we need half as many commands as padding bytes
                int len = padding / 2;
                Command[] commands = new Command[len];
                for (int i = 0; i < len; i++) {
                    commands[i] = Command.noop();
                }
                WsMessage msg = new WsCommandMessage(commands);
                session.write(msg);
            }
            else {
                session.removeAttribute(WsebAcceptor.CLIENT_PADDING_KEY);
            }
        }
    }

    private static final class CheckInitialPadding implements IoFutureListener<WriteFuture> {
        @Override
        public void operationComplete(WriteFuture future) {
            HttpAcceptSession session = (HttpAcceptSession)future.getSession();
            checkInitialPadding(session);
        }
    }

    private static void checkBlockPadding(HttpAcceptSession session) {
        // TODO: Verify if counting bytes is really necessary
        // check to see if we need to add a padding message to the end of sent messages
        long writtenBytes = session.getWrittenBytes();
        Long bytesWrittenOnLastFlush = (Long)session.getAttribute(WsebAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY);
        if (bytesWrittenOnLastFlush == null || writtenBytes != bytesWrittenOnLastFlush.longValue()) {
            // Block Padding is required
            session.write(WsebFrameEncoder.BLOCK_PADDING_MESSAGE);
            session.setAttribute(WsebAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY, writtenBytes + 4096);
        }
    }


    private static void checkBufferPadding(HttpAcceptSession parent, WsebSession wsebSession) {
        checkBlockPadding(parent);
        checkBuffer(parent, wsebSession);
    }

    private static void checkBuffer(HttpAcceptSession parent, WsebSession wsebSession) {
        if (parent.isClosing() || wsebSession.isReconnecting()) {
            return;
        }
        // check to see if we have written out at least enough bytes to be
        // over the client buffer
        Long clientBuffer = (Long)parent.getAttribute(WsebAcceptor.CLIENT_BUFFER_KEY);
        if (clientBuffer != null) {
            long bytesWritten = parent.getWrittenBytes()+parent.getScheduledWriteBytes();
            if (bytesWritten != 0 && bytesWritten >= clientBuffer) {
                // TODO: thread safety
                // multiple threads can trigger a reconnect on the same WsfSession
                if (wsebSession.compareAndSetReconnecting(false, true)) {
                    wsebSession.enqueueReconnectAndFlush();
                }
            }
        }
    }


}
