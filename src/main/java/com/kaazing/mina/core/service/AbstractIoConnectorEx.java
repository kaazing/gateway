/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.util.List;
import java.util.concurrent.Executor;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.core.write.WriteRequestEx;
import com.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public abstract class AbstractIoConnectorEx extends AbstractIoConnector implements IoConnectorEx {

    private final List<ThreadLocal<WriteRequestEx>> sharedWriteRequests = ShareableWriteRequest.initWithLayers(16);

    protected AbstractIoConnectorEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
    }

    @Override
    public ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer) {
        return sharedWriteRequests.get(ioLayer);
    }
}
