/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket;

import com.kaazing.mina.core.service.IoAcceptorEx;

/**
 * This interface extends SocketAcceptor in order to implement IoAcceptorEx
 */
public interface SocketAcceptorEx extends SocketAcceptor, IoAcceptorEx {

    @Override
    SocketSessionConfigEx getSessionConfig();

}
