/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import org.apache.mina.core.service.IoService;

import com.kaazing.mina.core.session.IoSessionConfigEx;

public interface IoServiceEx extends IoService  {

    @Override
    IoSessionConfigEx getSessionConfig();
}
