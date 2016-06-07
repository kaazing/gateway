/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.mina.netty;

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
