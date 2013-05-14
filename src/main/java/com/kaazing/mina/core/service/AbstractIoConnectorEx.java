/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.util.concurrent.Executor;

import com.kaazing.mina.core.session.IoSessionConfigEx;

public abstract class AbstractIoConnectorEx extends AbstractIoConnector implements IoConnectorEx {

    protected AbstractIoConnectorEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
    }
}
