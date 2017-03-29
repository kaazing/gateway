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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.mina.util.DefaultExceptionMonitor;
import org.kaazing.mina.util.ExceptionMonitor;


public class ExceptionMonitorTest {

    @Before
    public void setUp() {
        ExceptionMonitor.setInstance(null);
    }

    @Test
    public void getInstanceShouldReturnDefaultMonitorInitially() {
        assertTrue(ExceptionMonitor.getInstance().getClass().isAssignableFrom(DefaultExceptionMonitor.class));
    }

    @Test
    public void getInstanceShouldReturnLastSetInstance() {
        ExceptionMonitor expectedMonitor = new ExceptionMonitor() {
            @Override
            public void exceptionCaught0(Throwable cause, IoSession s) {
                //
            }
        };
        ExceptionMonitor.setInstance(expectedMonitor);
        assertEquals(expectedMonitor, ExceptionMonitor.getInstance());
    }

    @Test
    public void getInstanceShouldCallNettySetInstanceWithNonNullSession() {
        AtomicInteger exceptionCaught = new AtomicInteger(0);
        ExceptionMonitor nettyExpectedMonitor = new ExceptionMonitor() {
            @Override
            public void exceptionCaught0(Throwable cause, IoSession s) {
                exceptionCaught.set(1);
            }
        };
        org.apache.mina.util.ExceptionMonitor minaExpectedMonitor = new org.apache.mina.util.ExceptionMonitor() {
            @Override
            public void exceptionCaught(Throwable cause) {
                exceptionCaught.set(-1);
            }
        };
        ExceptionMonitor.setInstance(nettyExpectedMonitor);
        org.apache.mina.util.ExceptionMonitor.setInstance(minaExpectedMonitor);
        assertEquals(nettyExpectedMonitor, ExceptionMonitor.getInstance());
        assertEquals(minaExpectedMonitor, org.apache.mina.util.ExceptionMonitor.getInstance());
        ExceptionMonitor.getInstance().exceptionCaught(new Exception(), new DummySession());
        assertEquals(1, exceptionCaught.get());
        ExceptionMonitor.getInstance().exceptionCaught(new Exception(), null);
        assertEquals(-1, exceptionCaught.get());
    }

}
