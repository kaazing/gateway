/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.write;

import java.net.SocketAddress;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.future.WriteFutureEx;

/**
 * Extended version of WriteRequest to add support for mutating the
 * message during encoding to avoid undesirable allocation.
 */
public interface WriteRequestEx extends WriteRequest {

    void setMessage(Object message);

    WriteFutureEx getFuture();

    boolean isResetable();
    void reset(IoSession session, Object message);
    void reset(IoSession session, Object message, SocketAddress destination);
}
