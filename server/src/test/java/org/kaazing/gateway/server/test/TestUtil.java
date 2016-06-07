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
package org.kaazing.gateway.server.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import static org.junit.Assert.fail;

/**
 * Utility methods for use by unit tests
 */
public class TestUtil {

    public static void dispose(BridgeAcceptor... acceptors) {
        for (BridgeAcceptor acceptor : acceptors) {
            if (acceptor != null) {
                acceptor.dispose();
            }
        }
    }

    public static void dispose(BridgeConnector... connectors) {
        for (BridgeConnector connector : connectors) {
            if (connector != null) {
                connector.dispose();
            }
        }
    }

    public static void waitForLatch(CountDownLatch l,
                                    final int delay,
                                    final TimeUnit unit,
                                    final String failureMessage)
            throws InterruptedException {

        l.await(delay, unit);
        if (l.getCount() != 0) {
            fail(failureMessage);
        }
    }

}

