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
package org.kaazing.gateway.transport.sse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer;
import org.kaazing.gateway.transport.sse.bridge.filter.SseEncoder;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;

public class SseAcceptProcessor extends BridgeAcceptProcessor<SseSession> {

    private static final CheckInitialPadding CHECK_INITIAL_PADDING = new CheckInitialPadding();
    private static final WriteRequest RECONNECT_REQUEST = new DefaultWriteRequestEx(new Object());

    @Override
    protected void removeInternal(SseSession session) {
        IoSession parent = session.getParent();
        if (parent == null || parent.isClosing()) {
            // TODO: throw write to close session exception
            return;
        }

        // send long retry to the client so there is time to detect and
        // disconnect the listener
        SseMessage sseMessage = new SseMessage();
        sseMessage.setRetry(60000);
        parent.write(sseMessage);

        super.removeInternal(session);
    }

    @Override
    protected void flushInternal(final SseSession session) {
        // get parent and check if null (no attached http session)
        final HttpAcceptSession parent = (HttpAcceptSession)session.getParent();
        if (parent == null || parent.isClosing()) {
            return;
        }

        // store last write so we can observe it
        WriteFuture lastWrite = null;

        IoFilterChain filterChain = session.getFilterChain();

        // TODO: thread safety
    	// multiple threads can trigger a reconnect on the same SseSession
        final AtomicBoolean reconnecting = new AtomicBoolean(false);

        // we can still have a current write request during the transition between writers
        WriteRequest currentWriteRequest = session.getCurrentWriteRequest();
        if (currentWriteRequest != null) {
            session.setCurrentWriteRequest(null);
        }
        
        // get write request queue and process it
        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
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
                    if (session.isClosing() || parent.isClosing()) {
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
                        checkInitialPadding(parent);
                    }
                    break;
                }
            }
            else {
                currentWriteRequest = null;
            }

            // identity compare for our marker as a command to reconnect the
            // stream
            if (request == RECONNECT_REQUEST) {
                SseMessage sseMessage = new SseMessage();
                sseMessage.setReconnect(true);
                parent.write(sseMessage);
                parent.close(false);

                // thread safety
                // this code assumes single-threaded access (flushInternal)
                // otherwise there is a race for code already past
                // the parent != null check above

                // explicitly null the parent reference because
                // parent not closing until flush completes
                session.setParent(null);
                break;
            }

            // get message and compare to types we can process
            Object message = request.getMessage();
            if (message instanceof IoBufferEx) {
                IoBufferEx buf = (IoBufferEx) message;
                try {
                    // hold current remaining bytes so we know how much was
                    // written
                    int remaining = buf.remaining();

                    if (remaining == 0) {
                        throw new IllegalStateException("Unexpected empty buffer");
                    }
                    
                    // stop if parent already closing
                    if (parent.isClosing()) {
                        session.setCurrentWriteRequest(request);
                        break;
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
                    if (buf instanceof SseBuffer) {
                        // reuse previously constructed message if available
                        SseBuffer sseBuffer = (SseBuffer)buf;
                        SseMessage sseMessage = sseBuffer.getMessage();
                        if (sseMessage == null) {
                            // cache newly constructed message (atomic update)
                            SseMessage newSseMessage = new SseMessage();
                            newSseMessage.setData(buf);
                            if (sseBuffer.isAutoCache()) {
                                // buffer is cached on parent, continue with derived caching
                                newSseMessage.initCache();
                            }
                            boolean wasUpdated = sseBuffer.setMessage(newSseMessage);
                            sseMessage = wasUpdated ? newSseMessage : sseBuffer.getMessage();
                        }
                        // flush the buffer out to the session
                        lastWrite = flushNowInternal(parent, sseMessage, sseBuffer, filterChain, request);
                    }
                    else {
                        SseMessage sseMessage = new SseMessage();
                        sseMessage.setData(buf);
                        // flush the buffer out to the session
                        lastWrite = flushNowInternal(parent, sseMessage, buf, filterChain, request);
                    }

                    // increment session written bytes
                    int written = remaining;
                    session.increaseWrittenBytes(written, System.currentTimeMillis());

                    // if we are not already reconnecting then add a listener to
                    // the last write future
                    // so it can check the written bytes and compare to client
                    // buffer
                    if (reconnecting.get() == false) {
                        // Check whether we require block padding
                        boolean checkBlockPadding = (parent.getAttribute(SseAcceptor.CLIENT_BLOCK_PADDING_KEY) != null);
                        if (!checkBlockPadding) {
                        	lastWrite.addListener(new CheckBuffer(session, reconnecting));
                        }
                        else {
                            lastWrite.addListener(new CheckBufferAndBlockPadding(session, reconnecting));
                        }
                    }

                    // wrote the last bytes of the message to link the last
                    // write close to message close
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

    private static void checkInitialPadding(HttpAcceptSession session) {
        // check to see if we need to add a padding message to the end of
        // the sent messages
        Integer clientPadding = (Integer)session.getAttribute(SseAcceptor.CLIENT_PADDING_KEY);
        if (clientPadding != null) {
            long writtenBytes = session.getWrittenBytes();
            int padding = (int)(clientPadding - writtenBytes);
            if (padding > 0) {
                // a message is required
                SseMessage sseMessage = new SseMessage();
                int commentSize = padding - 3;
                // check if we need more then just an empty message
                if (commentSize > 0) {
                    sseMessage.setComment(Utils.fill(' ', commentSize));
                }
                session.write(sseMessage);
            }
            else {
                session.removeAttribute(SseAcceptor.CLIENT_PADDING_KEY);
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
        Long bytesWrittenOnLastFlush = ((Long)session.getAttribute(SseAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY));
        if (bytesWrittenOnLastFlush == null || writtenBytes != bytesWrittenOnLastFlush) {
            // Block Padding is required
            session.write(SseEncoder.BLOCK_PADDING_MESSAGE);
            session.setAttribute(SseAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY, writtenBytes + 4096);
        }
    }

    private static final class CheckBufferAndBlockPadding extends CheckBuffer {
        public CheckBufferAndBlockPadding(SseSession sseSession, AtomicBoolean reconnecting) {
            super(sseSession, reconnecting);
        }

        @Override
        public void operationComplete(WriteFuture future) {
            HttpAcceptSession session = (HttpAcceptSession)future.getSession();
            checkBlockPadding(session);

            super.operationComplete(future);
        }
    }

    private static class CheckBuffer implements IoFutureListener<WriteFuture> {

        private final SseSession sseSession;
        private final AtomicBoolean reconnecting;

        public CheckBuffer(SseSession sseSession, AtomicBoolean reconnecting) {
            this.sseSession = sseSession;
            this.reconnecting = reconnecting;
        }

        @Override
        public void operationComplete(WriteFuture future) {
            HttpAcceptSession parent = (HttpAcceptSession)future.getSession();
            if (parent.isClosing() || reconnecting.get() == true) {
                return;
            }
            // check to see if we have written out at least enough bytes to be
            // over the client buffer
            Long clientBuffer = (Long)parent.getAttribute(SseAcceptor.CLIENT_BUFFER_KEY);
            if (clientBuffer != null) {
                long bytesWritten = parent.getWrittenBytes();
                if (bytesWritten >= clientBuffer) {
                    // TODO: thread safety
                    // multiple threads can trigger a reconnect on the same SseSession
                	if (reconnecting.compareAndSet(false, true)) {
	                    WriteRequestQueue writeRequestQueue = sseSession.getWriteRequestQueue();
	                    writeRequestQueue.offer(sseSession, RECONNECT_REQUEST);
                	}
                }
            }
        }
    }

}
