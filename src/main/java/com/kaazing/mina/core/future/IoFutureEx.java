/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.future;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

public interface IoFutureEx extends IoFuture {

    boolean isResetable();

    void reset(IoSession session);

}
