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
package org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.jmock.Mockery;
import org.junit.Test;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class IdleTimeoutFilterTest {

    @Test
    public void onPostAddShouldSetIdleTimeout() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoSessionConfigEx config = context.mock(IoSessionConfigEx.class);

        context.checking(new Expectations() {
            {
                oneOf(filterChain).getSession(); will(returnValue(session));
                oneOf(session).getConfig(); will(returnValue(config));
                oneOf(config).setIdleTimeInMillis(IdleStatus.WRITER_IDLE, 100 - 100/4);
            }
        });

        IdleTimeoutFilter filter = new IdleTimeoutFilter(100);
        filter.onPostAdd(filterChain, "test", nextFilter);
        context.assertIsSatisfied();
    }

    @Test
    public void readerIdleShouldNotWritePong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).sessionIdle(session, IdleStatus.READER_IDLE);
            }
        });

        IdleTimeoutFilter filter = new IdleTimeoutFilter(100);
        filter.sessionIdle(nextFilter, session,  IdleStatus.READER_IDLE);
        context.assertIsSatisfied();
    }


    @Test
    public void writerIdleShouldWritePong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final WsPongMessage expected = new WsPongMessage();

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).sessionIdle(session, IdleStatus.WRITER_IDLE);
                oneOf(nextFilter).filterWrite(with(session), with(writeRequestWithMessage(expected)));
            }
        });

        IdleTimeoutFilter filter = new IdleTimeoutFilter(100);
        filter.sessionIdle(nextFilter, session,  IdleStatus.WRITER_IDLE);
        context.assertIsSatisfied();
    }

}
