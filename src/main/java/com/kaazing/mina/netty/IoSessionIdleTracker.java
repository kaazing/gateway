/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

public interface IoSessionIdleTracker {

    void addSession(final AbstractIoSessionEx session);

    void removeSession(final AbstractIoSessionEx session);

    void dispose();

}
