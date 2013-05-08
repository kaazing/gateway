/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration testing is often performed on repetitive addresses, where successive tests might easily
 * attempt to reuse the same server bind port and the same client ephemeral port, all on the same IP address,
 * say 127.0.0.1.
 *
 * However, closing a socket places that end of a TCP connection in TIME_WAIT state (in case of delayed packets)
 * for 2 * Maximum Segment Lifetime (MSL).  RFC793 specifies MSL as 2 minutes, but common values are 30 seconds,
 * 1 minute or 2 minutes.
 *
 * When a server socket is in TIME_WAIT state, subsequent binds to the same address fail until 2 * MSL expires.
 *
 * This utility class lets integration tests vary the ports they use, within a specified range, to avoid transient
 * bind problems caused when TIME_WAIT state is triggered by common integration test scenarios.
 *
 * See http://tinyurl.com/bpomoe2
 */
public final class PortUtil {

    private static final AtomicInteger value = new AtomicInteger();

    private PortUtil() {
    }

    public static int nextPort(int base, int range) {
        return base + (value.incrementAndGet() % range);
    }
}
