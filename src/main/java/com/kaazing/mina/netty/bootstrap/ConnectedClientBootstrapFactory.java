/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

class ConnectedClientBootstrapFactory implements ClientBootstrapFactory {

    @Override
    public ConnectedClientBootstrap createBootstrap() {
        return new ConnectedClientBootstrap();
    }

}
