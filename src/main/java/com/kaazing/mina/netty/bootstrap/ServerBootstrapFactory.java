/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

public interface ServerBootstrapFactory {

    ServerBootstrapFactory CONNECTED = new ConnectedServerBootstrapFactory();
    ServerBootstrapFactory CONNECTIONLESS = new ConnectionlessServerBootstrapFactory();

    ServerBootstrap createBootstrap();

}
