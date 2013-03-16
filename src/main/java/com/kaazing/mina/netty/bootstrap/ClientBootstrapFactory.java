/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

public interface ClientBootstrapFactory {

    ClientBootstrapFactory CONNECTED = new ConnectedClientBootstrapFactory();
    ClientBootstrapFactory CONNECTIONLESS = new ConnectionlessClientBootstrapFactory();

    ClientBootstrap createBootstrap();

}
