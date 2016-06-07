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
package org.kaazing.gateway.transport.ssl.bridge.filter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.util.CircularQueue;
import org.slf4j.Logger;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;

/**
 * A helper class using the SSLEngine API to decrypt/encrypt data.
 * <p/>
 * Each connection has a SSLEngine that is used through the lifetime of the connection.
 * We allocate buffers for use as the outbound and inbound network buffers.
 * These buffers handle all of the intermediary data for the SSL connection. To make things easy,
 * we'll require outNetBuffer be completely flushed before trying to wrap any more data.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class SslHandler {

    private final SslFilter parent;
    private final SSLContext sslContext;
    private final IoSessionEx session;
    private final Queue<IoFilterEvent> preHandshakeEventQueue = new CircularQueue<>();
    private final Queue<IoFilterEvent> filterWriteEventQueue = new ConcurrentLinkedQueue<>();
    private final Queue<IoFilterEvent> messageReceivedEventQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger messageReceivedEventQueueConcurrentGuard = new AtomicInteger(0);
    private final Logger logger;
    private SSLEngine sslEngine;

    /**
     * Encrypted data from the net
     */
    private IoBufferEx inNetBuffer;

    /**
     * Encrypted data to be written to the net
     */
    private IoBufferEx outNetBuffer;

    /**
     * Applicaton cleartext data to be read by application
     */
    private IoBufferEx appBuffer;

    /**
     * Empty buffer used during initial handshake and close operations
     */
    private final IoBufferEx emptyBuffer;

    private SSLEngineResult.HandshakeStatus handshakeStatus;
    private boolean initialHandshakeComplete;
    private boolean handshakeComplete;
    private boolean writingEncryptedData;
    private final IoBufferAllocatorEx<?> allocator;

    /**
     * Constuctor.
     *
     * @param sslc
     * @throws SSLException
     */
    public SslHandler(SslFilter parent, SSLContext sslContext, IoSessionEx session, Logger logger)
            throws SSLException {
        this.parent = parent;
        this.session = session;
        this.sslContext = sslContext;
        this.logger = logger;
        this.allocator = session.getBufferAllocator();
        this.emptyBuffer = allocator.wrap(allocator.allocate(0));
        init();
    }

    /**
     * Initialize the SSL handshake.
     *
     * @throws SSLException
     */
    public void init() throws SSLException {
        if (sslEngine != null) {
            // We already have a SSL engine created, no need to create a new one
            return;
        }

        InetSocketAddress peer = (InetSocketAddress) session
                .getAttribute(SslFilter.PEER_ADDRESS);
        
        // Create the SSL engine here
        if (peer == null) {
            sslEngine = sslContext.createSSLEngine();
        } else {
            sslEngine = sslContext.createSSLEngine(peer.getHostName(), peer.getPort());
        }

        // Initialize the engine in client mode if necessary
        sslEngine.setUseClientMode(parent.isUseClientMode());

        // Initialize the different SslEngine modes
        if (parent.isWantClientAuth()) {
            sslEngine.setWantClientAuth(true);
        }

        if (parent.isNeedClientAuth()) {
            sslEngine.setNeedClientAuth(true);
        }

        if (logger.isTraceEnabled()) {
            List<String> supportedCiphers = toCipherList(sslEngine.getSupportedCipherSuites());
            if (supportedCiphers != null) {
                logger.trace(String.format("Supported SSL/TLS ciphersuites:\n  %s", toCipherString(supportedCiphers)));

            } else {
                logger.trace("Supported SSL/TLS ciphersuites: none");
            }

            List<String> enabledCiphers = toCipherList(sslEngine.getEnabledCipherSuites());
            if (enabledCiphers != null) {
                logger.trace(String.format("Default enabled SSL/TLS ciphersuites:\n  %s", toCipherString(enabledCiphers)));

            } else {
                logger.trace("Enabled SSL/TLS ciphersuites: none");
            }

            if (sslEngine.getWantClientAuth() ||
                sslEngine.getNeedClientAuth()) {
                logger.trace(String.format("Client certificate verification %s", sslEngine.getNeedClientAuth() ? "REQUIRED" : "requested" ));
            }
        }

        // NOTE: We need to modify the MINA code here so that we can properly
        // handle the intersecting of configured and supported ciphersuites;
        // we can only do this AFTER the SSLEngine has been instantiated,
        // since different providers will support different ciphersuites.
 
        String[] enabledCipherSuites = parent.getEnabledCipherSuites();
        if (enabledCipherSuites != null) {
            // The list of enabled ciphers which can be set is the intersection
            // of the supported ciphers list and the list of ciphers to be
            // enabled.
            List<String> supportedCiphers = toCipherList(sslEngine.getSupportedCipherSuites());
            List<String> configuredCiphers = toCipherList(enabledCipherSuites);
            configuredCiphers.retainAll(supportedCiphers);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("SSL/TLS ciphersuites in use:\n  %s", toCipherString(configuredCiphers)));
            }

            String[] enabledCiphers = configuredCiphers.toArray(new String[configuredCiphers.size()]);
            sslEngine.setEnabledCipherSuites(enabledCiphers);
        }

        String[] protocols = parent.getEnabledProtocols();
        if (protocols == null || protocols.length == 0) {
            // Service didn't configure any protocols. Let us not include SSLv2, SSLv3 protocols
            // by default as there are known vulnerabilities
            protocols = removeSslProtocols(sslEngine.getSupportedProtocols());
        } else {
            boolean sslv3Enabled = isSslv3Enabled(protocols);
            if (sslv3Enabled && logger.isWarnEnabled()) {
                logger.warn("SSLv3 protocol is enabled. SSLv3 known to have vulnerabilities");
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("SSL/TLS enabled protocols are: %s", Arrays.asList(protocols)));
        }
        sslEngine.setEnabledProtocols(protocols);

        // TODO : we may not need to call this method...
        sslEngine.beginHandshake();
        
        handshakeStatus = sslEngine.getHandshakeStatus();

        handshakeComplete = false;
        initialHandshakeComplete = false;
        writingEncryptedData = false;
    }

    /**
     * Release allocated buffers.
     */
    public void destroy() {
        if (sslEngine == null) {
            return;
        }

        sslEngine.closeOutbound();

        if (outNetBuffer != null) {
            outNetBuffer.capacity(sslEngine.getSession().getPacketBufferSize(), allocator);
        } else {
            createOutNetBuffer(0);
        }

        try {
            do {
                outNetBuffer.clear();
            } while (sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf()).bytesProduced() > 0);

        } catch (SSLException e) {
            // Ignore.
        } finally {
            destroyOutNetBuffer();
        }

        /* As per the SSLEngine Javadoc, we are the application which is
         * initiating the closure (as far as we know), thus we only need to
         * call closeOutbound() and NOT call closeInbound().  Calling
         * closeInbound() in this situation only leads to scary-looking
         * (but spurious) exceptions at DEBUG level anyway.
         */

        /*
        try {
            sslEngine.closeInbound();

        } catch (SSLException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unexpected exception from SSLEngine.closeInbound().", e);
            }
        }
        */

        sslEngine = null;
        preHandshakeEventQueue.clear();
    }

    private void destroyOutNetBuffer() {
        outNetBuffer.free();
        outNetBuffer = null;
    }

    public SslFilter getParent() {
        return parent;
    }

    public IoSession getSession() {
        return session;
    }

    /**
     * Check we are writing encrypted data.
     */
    public boolean isWritingEncryptedData() {
        return writingEncryptedData;
    }

    /**
     * Check if handshake is completed.
     */
    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    public boolean isInboundDone() {
        return sslEngine == null || sslEngine.isInboundDone();
    }

    public boolean isOutboundDone() {
        return sslEngine == null || sslEngine.isOutboundDone();
    }

    /**
     * Check if there is any need to complete handshake.
     */
    public boolean needToCompleteHandshake() {
        return handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP && !isInboundDone();
    }

    public void schedulePreHandshakeWriteRequest(NextFilter nextFilter,
                                                 WriteRequest writeRequest) {
        preHandshakeEventQueue.add(new IoFilterEvent(nextFilter,
                IoEventType.WRITE, session, writeRequest));
    }

    public void flushPreHandshakeEvents() throws SSLException {
        IoFilterEvent scheduledWrite;

        while ((scheduledWrite = preHandshakeEventQueue.poll()) != null) {
            parent.filterWrite(scheduledWrite.getNextFilter(), session,
                    (WriteRequest) scheduledWrite.getParameter());
        }
    }

    public void scheduleFilterWrite(NextFilter nextFilter, WriteRequest writeRequest) {
        filterWriteEventQueue.add(new IoFilterEvent(nextFilter, IoEventType.WRITE, session, writeRequest));
    }

    public void scheduleMessageReceived(NextFilter nextFilter, Object message) {
        messageReceivedEventQueue.add(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_RECEIVED, session, message));
    }

    public void flushScheduledEvents() {
        // Fire events only when no lock is hold for this handler.
        if (Thread.holdsLock(this)) {
            return;
        }

        IoFilterEvent e;

        // We need synchronization here inevitably because filterWrite can be
        // called simultaneously and cause 'bad record MAC' integrity error.
        synchronized (this) {
            while ((e = filterWriteEventQueue.poll()) != null) {
                e.getNextFilter().filterWrite(session, (WriteRequest) e.getParameter());
            }
        }

        //
        // KG-3161: prevent parallel messageReceived calls going down the filter chain from different threads
        //
        if (messageReceivedEventQueueConcurrentGuard.getAndIncrement() == 0) {
            // We're the first thread to get here
            while ((e = messageReceivedEventQueue.poll()) != null) {
                e.getNextFilter().messageReceived(session, e.getParameter());
            }
        }
        // Handle case where some other thread added more data to the queue and failed to process it because 
        // of the previous guard check. This is similar to the flush processing in AbstractBridgeProcessor.
        if (messageReceivedEventQueueConcurrentGuard.decrementAndGet() == 0) {
            // We're the last thread to get here, make sure queue is fully processed
            while ((e = messageReceivedEventQueue.poll()) != null) {
                e.getNextFilter().messageReceived(session, e.getParameter());
            }
        }
    }

    /**
     * Call when data read from net. Will perform inial hanshake or decrypt provided
     * Buffer.
     * Decrytpted data reurned by getAppBuffer(), if any.
     *
     * @param buf        buffer to decrypt
     * @param nextFilter Next filter in chain
     * @throws SSLException on errors
     */
    public void messageReceived(NextFilter nextFilter, ByteBuffer buf) throws SSLException {
        // append buf to inNetBuffer
        if (inNetBuffer == null) {
            inNetBuffer = allocator.wrap(allocator.allocate(buf.remaining())).setAutoExpander(allocator);
        }

        inNetBuffer.put(buf);
        if (!handshakeComplete) {
            handshake(nextFilter);
        }

        // Application data will be in the same message as the handshake
        // during False Start
        if (handshakeComplete) {
            decrypt(nextFilter);
        }

        if (isInboundDone()) {
            // Rewind the MINA buffer if not all data is processed and inbound is finished.
            int inNetBufferPosition = inNetBuffer == null? 0 : inNetBuffer.position();
            buf.position(buf.position() - inNetBufferPosition);
            inNetBuffer = null;
        }
    }

    /**
     * Get decrypted application data.
     *
     * @return buffer with data
     */
    public IoBuffer fetchAppBuffer() {
        IoBufferEx appBuffer = this.appBuffer.flip();
        this.appBuffer = null;
        return (IoBuffer) appBuffer;
    }

    /**
     * Get encrypted data to be sent.
     *
     * @return buffer with data
     */
    public IoBuffer fetchOutNetBuffer() {
        IoBufferEx answer = outNetBuffer;
        if (answer == null) {
            return (IoBuffer) emptyBuffer;
        }

        outNetBuffer = null;
        return (IoBuffer) answer.shrink(allocator);
    }

    /**
     * Encrypt provided buffer. Encrypted data returned by getOutNetBuffer().
     *
     * @param src data to encrypt
     * @throws SSLException on errors
     */
    public void encrypt(ByteBuffer src) throws SSLException {
        if (!handshakeComplete) {
            throw new IllegalStateException();
        }

        if (!src.hasRemaining()) {
            if (outNetBuffer == null) {
                outNetBuffer = emptyBuffer;
            }
            return;
        }

        createOutNetBuffer(src.remaining());

        // Loop until there is no more data in src
        while (src.hasRemaining()) {

            SSLEngineResult result = sslEngine.wrap(src, outNetBuffer.buf());
            if (result.getStatus() == SSLEngineResult.Status.OK) {
                if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    doTasks();
                }
            } else if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                outNetBuffer.capacity(outNetBuffer.capacity() << 1, allocator);
                outNetBuffer.limit(outNetBuffer.capacity());
            } else {
                throw new SSLException("SSLEngine error during encrypt: "
                        + result.getStatus() + " src: " + src
                        + "outNetBuffer: " + outNetBuffer);
            }
        }

        outNetBuffer.flip();
    }

    /**
     * Start SSL shutdown process.
     *
     * @return <tt>true</tt> if shutdown process is started.
     *         <tt>false</tt> if shutdown process is already finished.
     * @throws SSLException on errors
     */
    public boolean closeOutbound() throws SSLException {
        if (sslEngine == null || !isHandshakeComplete() || sslEngine.isOutboundDone()) {
            return false;
        }

        sslEngine.closeOutbound();

        createOutNetBuffer(0);
        SSLEngineResult result;
        for (;;) {
            result = sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf());
            if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                outNetBuffer.capacity(outNetBuffer.capacity() << 1, allocator);
                outNetBuffer.limit(outNetBuffer.capacity());
            } else {
                break;
            }
        }

        if (result.getStatus() != SSLEngineResult.Status.CLOSED) {
            throw new SSLException("Improper close state: " + result);
        }
        outNetBuffer.flip();
        return true;
    }

    /**
     * Decrypt in net buffer. Result is stored in app buffer.
     *
     * @throws SSLException
     */
    private void decrypt(NextFilter nextFilter) throws SSLException {

        if (!handshakeComplete) {
            throw new IllegalStateException();
        }

        unwrap(nextFilter);
    }

    /**
     * @param res
     * @throws SSLException
     */
    private void checkStatus(SSLEngineResult res)
            throws SSLException {

        SSLEngineResult.Status status = res.getStatus();

        /*
        * The status may be:
        * OK - Normal operation
        * OVERFLOW - Should never happen since the application buffer is
        *      sized to hold the maximum packet size.
        * UNDERFLOW - Need to read more data from the socket. It's normal.
        * CLOSED - The other peer closed the socket. Also normal.
        */
        if (status != SSLEngineResult.Status.OK
                && status != SSLEngineResult.Status.CLOSED
                && status != SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            throw new SSLException("SSLEngine error during decrypt: " + status
                    + " inNetBuffer: " + inNetBuffer + "appBuffer: "
                    + appBuffer);
        }
    }

    /**
     * Perform any handshaking processing.
     */
    public void handshake(NextFilter nextFilter) throws SSLException {
        for (;;) {
            switch (handshakeStatus) {
                case FINISHED :
                    session.setAttribute(
                            SslFilter.SSL_SESSION, sslEngine.getSession());
                    handshakeComplete = true;

                    if (logger.isDebugEnabled()) {
                        SSLSession sslSession = sslEngine.getSession();
                        logger.debug(String.format("SSL session ID %s on transport session #%d %s: cipher %s, app buffer size %d, packet buffer size %d", 
                            sslSession.getId(), session.getId(), session, sslSession.getCipherSuite(), sslSession.getApplicationBufferSize(), sslSession.getPacketBufferSize()));
                    }

                    if (!initialHandshakeComplete
                            && session.containsAttribute(SslFilter.USE_NOTIFICATION)) {
                        // SESSION_SECURED is fired only when it's the first handshake.
                        // (i.e. renegotiation shouldn't trigger SESSION_SECURED.)
                        initialHandshakeComplete = true;
                        scheduleMessageReceived(nextFilter,
                                SslFilter.SESSION_SECURED);
                    }
                    
                    return;
                    
                case NEED_TASK :
                    handshakeStatus = doTasks();
                    break;
                    
                case NEED_UNWRAP :
                    // we need more data read
                    SSLEngineResult.Status status = unwrapHandshake(nextFilter);
                    
                    if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW &&
                            handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED ||
                            isInboundDone()) {
                        // We need more data or the session is closed
                        return;
                    }
                    
                    break;

                case NEED_WRAP :
                    // First make sure that the out buffer is completely empty. Since we
                    // cannot call wrap with data left on the buffer
                    if (outNetBuffer != null && outNetBuffer.hasRemaining()) {
                        return;
                    }

                    SSLEngineResult result;
                    createOutNetBuffer(0);
                    
                    for (;;) {
                        result = sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf());
                        if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                            outNetBuffer.capacity(outNetBuffer.capacity() << 1, allocator);
                            outNetBuffer.limit(outNetBuffer.capacity());
                        } else {
                            break;
                        }
                    }

                    outNetBuffer.flip();
                    handshakeStatus = result.getHandshakeStatus();
                    writeNetBuffer(nextFilter);
                    break;
            
                default :
                    throw new IllegalStateException("Invalid Handshaking State"
                            + handshakeStatus);
            }
        }
    }

    private void createOutNetBuffer(int expectedRemaining) {
        // SSLEngine requires us to allocate unnecessarily big buffer
        // even for small data.  *Shrug*
        int capacity = Math.max(
                expectedRemaining,
                sslEngine.getSession().getPacketBufferSize());

        if (outNetBuffer != null) {
            outNetBuffer.capacity(capacity, allocator);
        } else {
            outNetBuffer = allocator.wrap(allocator.allocate(capacity)).minimumCapacity(0);
        }
    }

    public WriteFuture writeNetBuffer(NextFilter nextFilter)
            throws SSLException {
        // Check if any net data needed to be writen
        if (outNetBuffer == null || !outNetBuffer.hasRemaining()) {
            // no; bail out
            return null;
        }

        // set flag that we are writing encrypted data
        // (used in SSLFilter.filterWrite())
        writingEncryptedData = true;

        // write net data
        WriteFutureEx writeFuture = null;

        try {
            IoBuffer writeBuffer = fetchOutNetBuffer();
            writeFuture = new DefaultWriteFutureEx(session);
            parent.filterWrite(nextFilter, session, new DefaultWriteRequestEx(
                    writeBuffer, writeFuture));

            // loop while more writes required to complete handshake
            while (needToCompleteHandshake()) {
                try {
                    handshake(nextFilter);
                } catch (SSLException ssle) {
                    SSLException newSsle = new SSLHandshakeException(
                            "SSL handshake failed.");
                    newSsle.initCause(ssle);
                    throw newSsle;
                }

                IoBuffer outNetBuffer = fetchOutNetBuffer();
                if (outNetBuffer != null && outNetBuffer.hasRemaining()) {
                    writeFuture = new DefaultWriteFutureEx(session);
                    parent.filterWrite(nextFilter, session,
                            new DefaultWriteRequestEx(outNetBuffer, writeFuture));
                }
            }
        } finally {
            writingEncryptedData = false;
        }

        return writeFuture;
    }

    private void unwrap(NextFilter nextFilter) throws SSLException {
        // Prepare the net data for reading.
        if (inNetBuffer != null) {
            inNetBuffer.flip();
        }

        if (inNetBuffer == null || !inNetBuffer.hasRemaining()) {
            return;
        }

        SSLEngineResult res = unwrap0();

        // prepare to be written again
        if (inNetBuffer.hasRemaining()) {
            inNetBuffer.compact();
        } else {
            inNetBuffer = null;
        }

        checkStatus(res);

        renegotiateIfNeeded(nextFilter, res);
    }

    private SSLEngineResult.Status unwrapHandshake(NextFilter nextFilter) throws SSLException {
        // Prepare the net data for reading.
        if (inNetBuffer != null) {
            inNetBuffer.flip();
        }

        if (inNetBuffer == null || !inNetBuffer.hasRemaining()) {
            // Need more data.
            return SSLEngineResult.Status.BUFFER_UNDERFLOW;
        }

        SSLEngineResult res = unwrap0();
        handshakeStatus = res.getHandshakeStatus();

        checkStatus(res);

        // If handshake finished, no data was produced, and the status is still ok,
        // try to unwrap more
        if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
                && res.getStatus() == SSLEngineResult.Status.OK
                && inNetBuffer.hasRemaining()) {
            res = unwrap0();

            // prepare to be written again
            if (inNetBuffer.hasRemaining()) {
                inNetBuffer.compact();
            } else {
                inNetBuffer = null;
            }

            renegotiateIfNeeded(nextFilter, res);
        } else {
            // prepare to be written again
            if (inNetBuffer.hasRemaining()) {
                inNetBuffer.compact();
            } else {
                inNetBuffer = null;
            }
        }

        return res.getStatus();
    }

    private void renegotiateIfNeeded(NextFilter nextFilter, SSLEngineResult res)
            throws SSLException {
        if (res.getStatus() != SSLEngineResult.Status.CLOSED
                && res.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW
                && res.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            // Renegotiation required.
            handshakeComplete = false;
            handshakeStatus = res.getHandshakeStatus();
            handshake(nextFilter);
        }
    }

    private SSLEngineResult unwrap0() throws SSLException {
        if (appBuffer == null) {
            appBuffer = allocator.wrap(allocator.allocate(inNetBuffer.remaining()));
        } else {
            appBuffer.expand(inNetBuffer.remaining(), allocator);
        }

        SSLEngineResult res;
        do {
            res = sslEngine.unwrap(inNetBuffer.buf(), appBuffer.buf());
            if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                appBuffer.capacity(appBuffer.capacity() << 1, allocator);
                appBuffer.limit(appBuffer.capacity());
                continue;
            }
        } while ((res.getStatus() == SSLEngineResult.Status.OK || res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) &&
                 (handshakeComplete && res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING ||
                  res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP));

        return res;
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private SSLEngineResult.HandshakeStatus doTasks() {
        /*
         * We could run this in a separate thread, but I don't see the need
         * for this when used from SSLFilter. Use thread filters in MINA instead?
         */
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            // TODO : we may have to use a thread pool here to improve the performances
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }

    private List<String> toCipherList(String[] names) {
        if (names == null ||
            names.length == 0) {
            return null;
        }

        List<String> list = new ArrayList<>(names.length);
        Collections.addAll(list, names);

        return list;
    }

    private String toCipherString(List<String> names) {
        if (names == null ||
            names.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append("  ").append(name).append("\n");
        }

        String cipherString = sb.toString().trim();
        return cipherString;
    }

    private String[] removeSslProtocols(String[] protocols) {
        List<String> protocolList = new ArrayList<>();
        for(String protocol : protocols) {
            // JSSE doesn't enable SSLv2, but allows SSLv3/TLSv1 hellos encapsulated in SSLv2Hello format
            // Should we also disable SSLv2Hello ?
            if (!(protocol.equals("SSLv3") || protocol.equals("SSLv2"))) {
                protocolList.add(protocol);
            }
        }
        return protocolList.toArray(new String[protocolList.size()]);
    }

    private boolean isSslv3Enabled(String[] protocols) {
        for(String protocol : protocols) {
            if (protocol.equals("SSLv3")) {
                return true;
            }
        }
        return false;
    }
}

