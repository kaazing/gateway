/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;

public interface MutableIoSession extends IoSession {

    void setHandler(IoHandler handler);

}
