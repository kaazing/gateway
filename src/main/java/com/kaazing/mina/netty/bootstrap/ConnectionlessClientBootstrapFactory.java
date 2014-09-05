/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

class ConnectionlessClientBootstrapFactory implements ClientBootstrapFactory {

    @Override
    public ConnectionlessClientBootstrap createBootstrap() {
        return new ConnectionlessClientBootstrap();
    }

}
