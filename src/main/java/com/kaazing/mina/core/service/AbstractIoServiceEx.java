/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.util.List;
import java.util.concurrent.Executor;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;
import com.kaazing.mina.core.write.WriteRequestEx;

public abstract class AbstractIoServiceEx extends AbstractIoService implements IoServiceEx  {

    private final List<ThreadLocal<WriteRequestEx>> sharedWriteRequests = ShareableWriteRequest.initWithLayers(16);

    protected AbstractIoServiceEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
    }

    @Override
    public final ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer) {
        return sharedWriteRequests.get(ioLayer);
    }

}
