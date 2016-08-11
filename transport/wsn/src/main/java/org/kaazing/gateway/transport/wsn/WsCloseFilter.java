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
package org.kaazing.gateway.transport.wsn;

import static java.lang.String.format;
import static org.kaazing.gateway.transport.wsn.WsnSession.SESSION_KEY;
import static org.kaazing.gateway.util.InternalSystemProperty.WS_CLOSE_TIMEOUT;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.util.WSMessageTooLongException;
import org.kaazing.gateway.util.Utf8Util;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.slf4j.Logger;

/* The purpose of this filter is to enforce the RFC 6455 behavior with regard
 * to CLOSE frames (see Section 1.4).
 *
 * When callers call session.close() on the WS session, we need to ensure
 * that a CLOSE frame is sent (if one has not been sent already).  If we
 * have received a CLOSE frame from the client already, then we can let the
 * close request continue on down the filter chain.  Otherwise, we wait
 * for the client's CLOSE frame, with an associated timer.
 *
 * Note: A new WsCloseFilter will be created for each new WS (and extended
 * handshake) session IoFilterChain; we currently are NOT using a singleton
 * for the WsCloseFilter (and thus are NOT using session attributes for
 * storing session information).  This class is (hopefully) small enough not
 * to warrant use of the singleton filter pattern.
 */
public class WsCloseFilter
    extends WsFilterAdapter
    implements Runnable {

    private final Logger logger;
    private final ScheduledExecutorService scheduler;

    private AtomicBoolean sentCloseFrame;
    private AtomicBoolean receivedCloseFrame;
    private AtomicBoolean timedOut;

    private ScheduledFuture<?> closeFuture;
    private NextFilter closeNextFilter;
    private IoSession closeSession;
    private long closeTimeout;

    public WsCloseFilter(WebSocketWireProtocol wsVersion,
                         Properties configuration,
                         Logger logger,
                         ScheduledExecutorService scheduler) {
        this.sentCloseFrame = new AtomicBoolean(false);
        this.receivedCloseFrame = new AtomicBoolean(false);
        this.timedOut = new AtomicBoolean(false);

        this.logger = logger;
        assert scheduler != null;
        this.scheduler = scheduler;

        // Check the configuration for the timeout to use
        this.closeTimeout = getCloseTimeout(configuration);
    }

    private long getCloseTimeout(Properties configuration) {
        String timeoutConfig = WS_CLOSE_TIMEOUT.getProperty(configuration);

        // The Utils.parseTimeInterval() method Does The Right Thing(tm) if
        // there is no property value configured for the given property name;
        // that's why we provide a default string parameter.
        long timeout = Utils.parseTimeInterval(timeoutConfig, TimeUnit.MILLISECONDS);
        if (timeout <= 0) {
            throw new IllegalArgumentException(format("%s property value \"%s\" is invalid, must be positive",
                    WS_CLOSE_TIMEOUT.getPropertyName(), timeout));
        }

        if (logger.isTraceEnabled()) {
            logger.trace(format("Using %s property of %d milliseconds for CLOSE frame timeouts",
                    WS_CLOSE_TIMEOUT.getPropertyName(), timeout));
        }

        return timeout;
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {

        // If we already sent a WsClose frame to the client, drop all write requests on the floor to ensure we don't
        // write any data after WS_CLOSE, because that is strongly discouraged in RFC 6455, and moreover it can cause
        // WriteToClosedSessionException (KG-7060).
        if (sentCloseFrame.get()) {
            return;
        }
        super.filterWrite(nextFilter, session, writeRequest);
    }

    @Override
    public void messageReceived(NextFilter nextFilter,
                                IoSession session,
                                Object message)  throws Exception {

        // If we already received a WsClose from the client, drop this message on the floor because the client
        // should not be sending data after WS_CLOSE, because that is strongly discouraged in RFC 6455, and the
        // RFC says we should ignore any such data.
        if (receivedCloseFrame.get()) {
            return;
        }

        // If not a WsMessage, just forward it on.  This can happen e.g.
        // during testing, or during an extended handshake.  We want to
        // make sure that this filter does not interfere with the flow
        // of messages unduly.

        if (!(message instanceof WsMessage)) {
            nextFilter.messageReceived(session, message);

        } else {
            super.messageReceived(nextFilter, session, message);
        }
    }

    @Override
    protected void wsCloseReceived(final NextFilter nextFilter,
                                   final IoSession session,
                                   final WsCloseMessage wsClose)
        throws Exception {
        ByteBuffer reason = wsClose.getReason();
        int status = wsClose.getStatus();

        if (receivedCloseFrame.compareAndSet(false, true)) {
            if (logger != null &&
                logger.isTraceEnabled()) {
                logger.trace(format("received CLOSE frame from peer: %s", wsClose));
            }

            if (sentCloseFrame.get() == false) {
                // Echo the CLOSE frame back, and close the session.
                WsCloseMessage wsCloseResponse = new WsCloseMessage(status, reason);

                // if the message has invalid UTF-8 reason, just send back 1002 - protocol error
                if (status == WsCloseMessage.NORMAL_CLOSE.getStatus()) {
                    if (!Utf8Util.validBytesUTF8(reason, reason.position(), reason.limit())) {
                        wsCloseResponse = WsCloseMessage.PROTOCOL_ERROR;
                    }
                }
                WriteFutureEx writeFuture = new DefaultWriteFutureEx(session);
                WriteRequestEx writeRequest = new DefaultWriteRequestEx(wsCloseResponse, writeFuture);

                writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        if (future.isWritten()) {

                            // We both sent and received CLOSE frames: close
                            // the session.
                            if (logger != null &&
                                logger.isTraceEnabled()) {
                                    logger.trace("received and sent CLOSE frames, closing session");
                            }

                            if (closeFuture != null &&
                                closeFuture.isDone() == false) {
                                closeFuture.cancel(true);
                            }

                            WsnSession wsnSession = SESSION_KEY.get(session);
                            wsnSession.getCloseFuture().setClosed();
                            wsnSession.getProcessor().remove(wsnSession);
                        }
                    }
                });

                super.filterWrite(nextFilter, session, writeRequest);

            } else {
                // We both sent and received CLOSE frames: close the session.
                if (logger != null &&
                    logger.isTraceEnabled()) {
                    logger.trace("received and sent CLOSE frames, closing session");
                }

                if (closeFuture != null &&
                    closeFuture.isDone() == false) {
                    closeFuture.cancel(true);
                }

                nextFilter.filterClose(session);
            }

        } else {
            // We should NOT receive multiple CLOSE frames from the client, so
            // this case should not happen.

            if (logger != null &&
                logger.isTraceEnabled()) {
                logger.trace(format("received redundant CLOSE frame %s for session %s", wsClose, session));
            }
        }
    }

    @Override
    protected Object doFilterWriteWsClose(final NextFilter nextFilter,
                                          final IoSession session,
                                          WriteRequest writeRequest,
                                          WsCloseMessage message)
        throws Exception {

        if (!session.isConnected()) {
            if (logger != null && logger.isTraceEnabled()) {
                logger.trace("session is no longer connected - skipping WS CLOSE handshake");
            }
            nextFilter.filterClose(session);
            return null;
        }

        if (sentCloseFrame.compareAndSet(false, true)) {
            if (receivedCloseFrame.get() == false) {
                // Start the scheduled task, to limit the amount of time that
                // we wait for the peer's CLOSE.  If we don't receive the CLOSE
                // in time, we terminate the session anyway.

                if (logger != null && logger.isTraceEnabled()) {
                    logger.trace(format("sending WS CLOSE frame %s, then waiting %d milliseconds for peer CLOSE", message, closeTimeout));
                }
                closeNextFilter = nextFilter;
                closeSession = session;
                closeFuture = scheduler.schedule(WsCloseFilter.this, closeTimeout, TimeUnit.MILLISECONDS);

                WriteFuture writeFuture = writeRequest.getFuture();
                writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        if (future.isWritten()) {
                            session.suspendWrite();
                        }
                        else {
                            if (logger != null && logger.isTraceEnabled()) {
                                logger.trace(format(
                                        "write WS CLOSE frame failed with exception %s, calling nextFilter.filterClose",
                                        future.getException()));
                            }
                            nextFilter.filterClose(session);
                        }
                    }
                });

            } else {
                if (logger != null &&
                    logger.isTraceEnabled()) {
                    logger.trace(format("sending WS CLOSE frame %s", message));
                }
            }

            return message;

        } else {

            // If we have already sent a CLOSE frame, do not send another
            // one.  This defensive guard allows for custom CLOSE frames to
            // be sent by higher layers, if needed, as well as guards against
            // higher layer handlers from inadvertently sending CLOSE again.

            if (logger != null &&
                logger.isDebugEnabled()) {
                logger.debug(format("attempted to write redundant CLOSE frame %s", message));
            }
        }

        return null;
    }

    @Override
    public void filterClose(final NextFilter nextFilter,
                            final IoSession session)
        throws Exception {

        // If we've timed out, propagate the close.
        if (timedOut.get() == true) {
            nextFilter.filterClose(session);
            return;
        }

        if (!session.isConnected()) {
            if (logger != null && logger.isTraceEnabled()) {
                logger.trace("session is no longer connected - skipping WS CLOSE handshake");
            }
            nextFilter.filterClose(session);
            return;
        }

        // If we haven't sent a CLOSE frame already, send one.
        if (sentCloseFrame.get() == false) {
            // Note: do NOT use nextFilter.filterWrite() here; we explicitly
            // want our own doFilterWriteWsClose() method to be called.

            WsnSession wsnSession = SESSION_KEY.get(session);
            // SESSION_KEY attribute with wsnSession may not be populated when the
            // sessionOpened() in upstream closes the session.
            Throwable cause = wsnSession == null ? null : wsnSession.getCloseException();
            WsCloseMessage closeMessage;
            if (cause != null) {
                if (cause instanceof WSMessageTooLongException) {
                    closeMessage = WsCloseMessage.MESSAGE_TOO_LONG_ERROR;
                    session.suspendRead();
                } else if(cause instanceof ProtocolDecoderException) {
                    closeMessage = WsCloseMessage.PROTOCOL_ERROR;
                } else {
                    closeMessage = WsCloseMessage.NORMAL_CLOSE;
                }
            } else {
                closeMessage = WsCloseMessage.NORMAL_CLOSE;
            }
            WriteFutureEx writeFuture = new DefaultWriteFutureEx(session);
            WriteRequestEx writeRequest = new DefaultWriteRequestEx(closeMessage, writeFuture);

            // If we have already received the peer's CLOSE, then close
            // this session once we've written out our CLOSE.
            if (receivedCloseFrame.get() == true) {
                writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        if (future.isWritten()) {
                            if (closeFuture != null &&
                                closeFuture.isDone() == false) {
                                closeFuture.cancel(true);
                            }

                            nextFilter.filterClose(session);
                        }
                        else {
                            if (logger != null && logger.isTraceEnabled()) {
                                logger.trace(format(
                                        "write WS CLOSE frame failed with exception %s, calling nextFilter.filterClose",
                                        future.getException()));
                            }
                            nextFilter.filterClose(session);
                        }
                    }
                });
            }

            super.filterWrite(nextFilter, session, writeRequest);

        } else {
          // If we have received the peer's CLOSE, then we can actually
          // close this session.
          if (receivedCloseFrame.get() == true) {
              if (closeFuture != null &&
                  closeFuture.isDone() == false) {
                closeFuture.cancel(true);
              }

              nextFilter.filterClose(session);
          }
        }
    }

    @Override
    public void onPreRemove(IoFilterChain filterChain,
                            String name,
                            NextFilter nextFilter)
        throws Exception {

        // Make sure to cancel and clear any pending close tasks
        if (closeFuture != null) {
            if (closeFuture.isDone() == false) {
                closeFuture.cancel(true);
            }

            closeFuture = null;
        }
    }

    @Override
    public void run() {
        try {
            // Terminate with extreme prejudice
            timedOut.set(true);

            if (logger != null &&
                logger.isInfoEnabled()) {
                logger.info(format("terminating session %s after waiting for %d milliseconds for CLOSE frame", closeSession, closeTimeout));
            }

            /* Note that the session being closed here is NOT the WS session,
             * but rather the underlying TCP connection.
             *
             * In order to properly terminate this session, though, we CANNOT
             * use session.close() (true or false, doesn't matter).  Why not?
             * Because MINA's IoSession class will check session.isClosing()
             * first.  If the session IS already closing, then the close
             * request will NOT be sent down the filter chain.  And the fact
             * that we're in this task that fired means that session.close()
             * was called, and thus that session.isClosing() returns true.
             *
             * How, then, to get the close request down?  We keep a hold
             * of the NextFilter, and then we call NextFilter.filterClose().
             * And THAT check bypasses the session.isClosing() sanity check,
             * so that our close request proceeds.
             */
            closeNextFilter.filterClose(closeSession);

        } catch (Throwable t) {
            if (logger != null) {
                if (logger.isDebugEnabled()) {
                    logger.warn(format("Error closing session %s after %d milliseconds: %s", closeSession, closeTimeout, t), t);

                } else {
                    logger.warn(format("Error closing session %s after %d milliseconds: %s", closeSession, closeTimeout, t));
                }
            }
        }
    }

    public static boolean neededForProtocolVersion(WebSocketWireProtocol wsVersion) {
        return WebSocketWireProtocol.RFC_6455 == wsVersion ||
                WebSocketWireProtocol.HYBI_13 == wsVersion ||
                WebSocketWireProtocol.HYBI_8 == wsVersion ||
                WebSocketWireProtocol.HYBI_7 == wsVersion ||
                WebSocketWireProtocol.HYBI_6 == wsVersion;

    }
}
