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

import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.kaazing.mina.util.DefaultExceptionMonitor;
import org.kaazing.mina.util.ExceptionMonitor;


public class ExceptionMonitorTest
{
    @Test
    public void getInstanceShouldReturnDefaultMonitorInitially(){
        assertTrue(ExceptionMonitor.getInstance().getClass().isAssignableFrom(DefaultExceptionMonitor.class));
    }

    @Test
    public void getInstanceShouldReturnLastSetInstance () {
        ExceptionMonitor expectedMonitor = new ExceptionMonitor() {
            @Override
            public void exceptionCaught(Throwable cause, IoSession s) {
                //
            }
        };
        ExceptionMonitor.setInstance(expectedMonitor);

        assertEquals(expectedMonitor, ExceptionMonitor.getInstance());
    }
}
