/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import com.kaazing.mina.core.session.IoSessionEx;

public interface IoSessionIdleTracker {

    void addSession(final IoSessionEx session);

    void removeSession(final IoSessionEx session);

    void dispose();

}
