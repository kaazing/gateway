/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.util.concurrent.Executor;

import com.kaazing.mina.core.session.IoSessionConfigEx;

public abstract class AbstractIoServiceEx extends AbstractIoService implements IoServiceEx  {

    protected AbstractIoServiceEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
    }

}
