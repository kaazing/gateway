/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListenerSupport;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.core.write.WriteRequestEx;

public interface IoServiceEx extends IoService  {

    @Override
    IoSessionConfigEx getSessionConfig();

    IoServiceListenerSupport getListeners();

    ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer);
}
