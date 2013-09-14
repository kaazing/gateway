/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

class ConnectedServerBootstrapFactory implements ServerBootstrapFactory {

    @Override
    public ConnectedServerBootstrap createBootstrap() {
        return new ConnectedServerBootstrap();
    }

}
