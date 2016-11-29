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
package org.kaazing.gateway.transport.http;

import static java.lang.String.valueOf;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpUtils.formatDateHeader;

import java.util.Queue;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.BridgeAcceptHandler;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.UpgradeFuture;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer;
import org.kaazing.gateway.transport.http.bridge.filter.HttpCodecFilter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpAcceptProcessor extends BridgeAcceptProcessor<DefaultHttpSession> {

    private static final IoFutureListener<CommitFuture> WRITE_RESUMER = new WriteResumer();
    private static final IoFutureListener<CommitFuture> UPGRADER = new Upgrader();

    @Override
    protected void add0(DefaultHttpSession session) {
        super.add0(session);

        CommitFuture commitFuture = session.getCommitFuture();
        commitFuture.addListener(UPGRADER);
    }

    @Override
    protected void removeInternal(final DefaultHttpSession session) {
        CommitFuture future = session.commit();
        if (future.isCommitted()) {
            removeInternal0(session);
        } else {
            future.addListener(new IoFutureListener<CommitFuture>() {
                @Override
                public void operationComplete(CommitFuture future) {
                    if (future.isCommitted()) {
                        removeInternal0(session);
                    }
                }
            });
        }
    }

    private void removeInternal0(DefaultHttpSession session) {
        IoSession parent = session.getParent();
        if (parent == null || parent.isClosing()) {
            return;
        }

            boolean connectionClose = session.isConnectionClose();
            if (connectionClose) {
                // close TCP connection when write complete
                parent.close(false);
            }
            else {
                // write the empty chunk
                IoBufferAllocatorEx<? extends HttpBuffer> allocator = session.getBufferAllocator();
                HttpBuffer unsharedEmpty = allocator.wrap(allocator.allocate(0));
                HttpContentMessage completeMessage = new HttpContentMessage(unsharedEmpty, true, session.isChunked(), session.isGzipped());
                parent.write(completeMessage);
            }
        }

    public void commit(final DefaultHttpSession session) {
        // get and verify we have a parent that is open
        final IoSession parent = session.getParent();
        if (parent == null || parent.isClosing()) {
            return;
        }

        // verify we are not already committed
        final CommitFuture commitFuture = session.getCommitFuture();
        if (commitFuture.isCommitted()) {
            return;
        }

        // create HttpResponseMessage
        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setStatus(session.getStatus());
        httpResponse.setReason(session.getReason());
        httpResponse.setVersion(session.getVersion());
        httpResponse.setHeaders(session.getWriteHeaders());
        httpResponse.setInjectableHeaders(session.getLocalAddress().getOption(INJECTABLE_HEADERS));
        httpResponse.setCookies(session.getWriteCookies());
        httpResponse.setBlockPadding(session.getParameter(".kbp") != null);
        // KG-5615: committed response cannot be complete if a write request is still pending in the write request queue
        boolean complete = (session.getCurrentWriteRequest() == null) && session.getWriteRequestQueue().isEmpty(session) && session.isClosing();
        IoBufferAllocatorEx<? extends HttpBuffer> allocator = session.getBufferAllocator();
        HttpBuffer unsharedEmpty = allocator.wrap(allocator.allocate(0));
        httpResponse.setContent(new HttpContentMessage(unsharedEmpty, complete));
        if (session.getMethod() == HttpMethod.HEAD) {
            httpResponse.setContentExcluded(true);
        }

        switch (session.getVersion()) {
        case HTTP_1_1:
            boolean isConnectionClose = "close".equals(session.getWriteHeader("Connection"));

            // Not checking for previously written bytes on keep-alive connections
            if (isConnectionClose) {
                // [REMOVED CHECK FOR THE FOLLOWING]
                // Note: if this was not the first HTTP response for the underlying TCP connection, then it could
                // be pipelined already before we get the chance to send Connection:close, hence check writtenBytes = 0
                writeNonPersistentResponse(session, parent, commitFuture, httpResponse);
            }
            else {
                if (!httpResponse.isComplete()) {
                    String contentLength = session.getWriteHeader(HEADER_CONTENT_LENGTH);
                    if (contentLength == null) {
                        if (session.isClosing() || session.isWriteShutdown()) {
                            long scheduledWriteBytes = session.getScheduledWriteBytes();
                            httpResponse.setHeader(HEADER_CONTENT_LENGTH, valueOf(scheduledWriteBytes));
                        }
                        else if (!httpResponse.isContentLengthImplicit()) {
                            // if content-length is not specified, then chunking is necessary
                            // unless session indicates otherwise
                            if ( session.isChunkingNecessary() ) {
                                httpResponse.setHeader("Transfer-Encoding", "chunked");
                                session.setChunked(true);
                            }
                        }
                    }
                }
                writePersistentResponse(parent, commitFuture, httpResponse);
            }
            break;
        case HTTP_1_0:
            writeNonPersistentResponse(session, parent, commitFuture, httpResponse);
            break;
        default:
            throw new IllegalStateException("Unexpected HTTP version: " + session.getVersion());
        }
    }

    private void writePersistentResponse(final IoSession parent, final CommitFuture commitFuture, HttpResponseMessage httpResponse) {
        WriteFuture writeFuture = parent.write(httpResponse);
        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
            @Override
            public void operationComplete(WriteFuture future) {
                commitFuture.setCommited();
            }
        });
    }

    private void writeNonPersistentResponse(final DefaultHttpSession session, final IoSession parent,
            final CommitFuture commitFuture, final HttpResponseMessage httpResponse) {
        WriteFuture writeFuture = parent.write(httpResponse);
        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
            @Override
            public void operationComplete(WriteFuture future) {
                // if request pipelining is disabled, then chunking is not necessary and response cannot be reused
                try {
                    // Remove all HTTP filters except codec and logging filter (type agnostic)
                    IoFilterChain filterChain = parent.getFilterChain();
                    Entry codec = filterChain.getEntry(HttpCodecFilter.class);
                    Entry logging = filterChain.getEntry(ObjectLoggingFilter.class);

                    filterChain.clear();

                    // Attach filter for content-encoding/transfer-encoding
                    boolean isChunked = HttpUtils.isChunked(httpResponse);
                    boolean isGzipped = HttpUtils.isGzipped(httpResponse);

                    // Add back the codec filter only if further encoding required downstream
                    if (isChunked || isGzipped) {
                        session.setChunked(isChunked);
                        session.setGzipped(isGzipped);
                        // Use add last to make sure it doesn't get in front of TrafficShapingFilter (which
                        // automatically re-adds itself when the filter chain is cleared)
                        filterChain.addLast(codec.getName(), codec.getFilter());
                    }

                    if (logging != null) {
                        filterChain.addLast(logging.getName(), logging.getFilter());
                    }
                }
                catch (Exception e) {
                    parent.getFilterChain().fireExceptionCaught(e);
                }

                // mark HTTP session as connection close to permit optimizations
                session.setConnectionClose();

                // Note: commit future must be completed after filter chain is cleared
                // to trigger subsequent flush with verbatim content
                commitFuture.setCommited();
            }
        });
    }

    @Override
    protected void consume(DefaultHttpSession session) {
        if (session.isReadSuspended()) {
            return;
        }
        Queue<IoBufferEx> deferredReads = session.getDeferredReads();
        IoBufferEx buffer;
        while ( (buffer = deferredReads.poll()) != null) {
            if (buffer.hasRemaining()) {
                // direct read for now, in the future this should always get buffered
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireMessageReceived(buffer);
            }
        }
    }

    @Override
    protected WriteFuture flushNow(DefaultHttpSession session, IoSessionEx parent,
            IoBufferEx buf, IoFilterChain filterChain, WriteRequest request) {
        // thread safety
        // ensure commit is at least in progress
        // schedule writes after commit completes
        CommitFuture commitFuture = session.commit();
        if (!commitFuture.isCommitted()) {
            // suspend before resume (in case commit future now complete)
            session.suspendWrite();
            // resume write triggers a flush if previously not writable
            commitFuture.addListener(WRITE_RESUMER);
            // cancel flush until commit future has completed
            return null;
        }

        boolean isGzipped = session.isGzipped();
        boolean isChunked = session.isChunked();
        if (session.isConnectionClose() && !isChunked && !isGzipped) {
            // write IoBuffer directly when pipelining disabled
            return super.flushNow(session, parent, buf, filterChain, request);
        }
        else if (buf instanceof HttpBuffer) {
            // write out message buffer
            // reuse previously constructed message if available
            HttpBuffer httpBuffer = (HttpBuffer) buf;

            // Fetch a message with the same encoding as required by this session
            String key = HttpBuffer.getEncodingKey(isGzipped, isChunked);
            HttpMessage httpMessage = httpBuffer.getMessage(key);
            if (httpMessage == null) {
                // cache newly constructed message (atomic update)
                HttpContentMessage newHttpMessage = new HttpContentMessage(buf, false, isChunked, isGzipped);
                if (httpBuffer.isAutoCache()) {
                    // Note: this code path only occurs if we need further encoding
                    //       for downstream with Connection:close header
                    // buffer is cached on parent, continue with derived caching
                    newHttpMessage.initCache();
                }

                // Put the new message in the buffer, and use whichever is stored there
                httpMessage = httpBuffer.putMessage(key, newHttpMessage);
            }
            return flushNowInternal(parent, httpMessage, httpBuffer, filterChain, request);
        }
        else {
            return flushNowInternal(parent, new HttpContentMessage(buf, false, isChunked, isGzipped), buf, filterChain, request);
        }
    }

    private static final class WriteResumer implements IoFutureListener<CommitFuture> {
        @Override
        public void operationComplete(CommitFuture future) {
            IoSession session = future.getSession();
            session.resumeWrite();
        }
    }

    private static final class Upgrader implements IoFutureListener<CommitFuture> {

        private final Logger logger = LoggerFactory.getLogger("transport.http");

        @Override
        public void operationComplete(CommitFuture future) {
            final DefaultHttpSession session = (DefaultHttpSession) future.getSession();
            if (session.getStatus() == HttpStatus.INFO_SWITCHING_PROTOCOLS) {
                CloseFuture closeFuture = session.getCloseFuture();
                closeFuture.addListener(new IoFutureListener<CloseFuture>() {
                    @Override
                    public void operationComplete(CloseFuture future) {
                        IoSession parent = session.getParent();
                        if (parent == null || parent.isClosing()) {
                            return;
                        }
                        final UpgradeFuture upgradeFuture = session.getUpgradeFuture();
                        final IoHandler upgradeHandler = session.getUpgradeHandler();
                        if (upgradeHandler != null) {
                            IoHandler oldHandler;
                            if (parent instanceof AbstractBridgeSession<?, ?>) {
                                // SSL
                                AbstractBridgeSession<?, ?> bridgeParent = (AbstractBridgeSession<?, ?>) parent;
                                oldHandler = bridgeParent.getHandler();
                                bridgeParent.setHandler(upgradeHandler);
                            }
                            //TODO: Change IoSessionAdapterEx to use AbstractBridgeSesions for TCP layers with transports.
                            else if (parent instanceof IoSessionAdapterEx) {
                                // TCP bridged with transport underneath
                                oldHandler = parent.getHandler();
                                ((IoSessionAdapterEx) parent).setHandler(upgradeHandler);
                            }
                            else {
                                // TCP
                                oldHandler = (IoHandler)parent.setAttribute(BridgeAcceptHandler.DELEGATE_KEY, upgradeHandler);
                            }
                            try {
                                oldHandler.sessionClosed(parent);
                                upgradeFuture.setUpgraded();
                                upgradeHandler.sessionOpened(parent);
                            }
                            catch (Exception e) {
                                logger.error("Exception during upgrade", e);
                                parent.close(true);
                            }
                        }
                    }
                });
            }
        }
    }

    public static void setServerHeader(IoSession session, HttpResponseMessage response) {
        DefaultHttpSession httpSession = HttpAcceptor.SESSION_KEY.get(session);
        setServerHeader(httpSession, response);
    }

    public static void setDateHeader(IoSession session, HttpResponseMessage response) {
        DefaultHttpSession httpSession = HttpAcceptor.SESSION_KEY.get(session);
        if(httpSession != null) {
            ResourceAddress address = httpSession.getLocalAddress();
            boolean dateHeaderEnabled = address.getOption(HttpResourceAddress.DATE_HEADER_ENABLED);
            if(dateHeaderEnabled && !response.hasHeader("Date")) {
                response.setHeader(HttpHeaders.HEADER_DATE, formatDateHeader(System.currentTimeMillis()));
            }
        }
    }

    public static void setServerHeader(DefaultHttpSession httpSession, HttpResponseMessage httpResponse) {
        if (httpSession != null) {
            ResourceAddress address = httpSession.getLocalAddress();
            boolean serverHeaderEnabled = address.getOption(HttpResourceAddress.SERVER_HEADER_ENABLED);
            if (serverHeaderEnabled && !httpResponse.hasHeader("Server")) {
                httpResponse.setHeader("Server", "Kaazing Gateway");
            }
        }
    }
}
