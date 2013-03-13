/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.net.SocketAddress;

import org.apache.mina.core.service.IoAcceptor;

import com.kaazing.mina.core.future.BindFuture;

public interface IoAcceptorEx extends IoAcceptor, IoServiceEx  {

    /**
     * Binds to the specified local address and start to accept incoming
     * connections.
     *
     * @return A BindFuture if failed to bind
     */
    BindFuture bindAsync(SocketAddress localAddress);

}
