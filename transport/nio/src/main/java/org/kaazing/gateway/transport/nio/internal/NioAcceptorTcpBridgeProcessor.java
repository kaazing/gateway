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
package org.kaazing.gateway.transport.nio.internal;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;

public class NioAcceptorTcpBridgeProcessor implements IoProcessorEx<IoSessionAdapterEx> {

    //
    // Tcp as a "virtual" bridge session when we specify tcp.transport option in a resource address.
    //
    public static final String TCP_SESSION_KEY = "tcp.bridgeSession.key"; // holds tcp session acting as a bridge
    public static final String PARENT_KEY = "tcp.parentKey.key"; // holds parent of tcp bridge session

    private final IoAcceptorEx acceptor;

    NioAcceptorTcpBridgeProcessor(IoAcceptorEx acceptor) {
        this.acceptor = acceptor;
    }


    @Override
    public void add(IoSessionAdapterEx session) {
        // Do nothing
    }

    @Override
    public void flush(IoSessionAdapterEx session) {
        IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
        WriteRequest req = session.getWriteRequestQueue().poll(session);

        // Check that the request is not null. If the session has been closed,
        // we may not have any pending requests.
        if (req != null) {
            final WriteFuture tcpBridgeWriteFuture = req.getFuture();
            Object m = req.getMessage();
            if (m instanceof IoBufferEx && ((IoBufferEx) m).remaining() == 0) {
                session.setCurrentWriteRequest(null);
                tcpBridgeWriteFuture.setWritten();
            } else {
                WriteFuture parentWriteFuture = parent.write(m);
                parentWriteFuture.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        if (future.isWritten()) {
                            tcpBridgeWriteFuture.setWritten();
                        } else {
                            tcpBridgeWriteFuture.setException(future.getException());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void remove(IoSessionAdapterEx session) {
        AbstractNioAcceptor.LOG.debug("AbstractNioAcceptor Fake Processor remove session " + session);
        IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
        parent.close(false);
        acceptor.getListeners().fireSessionDestroyed(session);
    }

    @Override
    public void updateTrafficControl(IoSessionAdapterEx session) {
        // Do nothing
    }

    @Override
    public void dispose() {
        // Do nothing
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public boolean isDisposing() {
        return false;
    }

}
