/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;

public class DefaultIoConnectorChannelHandlerFactory
       implements IoConnectorChannelHandlerFactory {

    @Override
    public IoConnectorChannelHandler createHandler(ChannelIoConnector<?, ?, ?> connector, ConnectFuture connectFuture,
                                                   IoSessionInitializer<?> sessionInitializer) {
        return new IoConnectorChannelHandler(connector, connectFuture, sessionInitializer);
    }

}
