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
package org.kaazing.gateway.transport.http.bridge.filter;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.test.util.Mockery;

public class HttpContentLengthAdjustementFilterTest {

    private static final SimpleBufferAllocator BUFFER_ALLOCATOR = SimpleBufferAllocator.BUFFER_ALLOCATOR;
    private Mockery context = new Mockery();
    private HttpSession session = context.mock(HttpSession.class);
    private NextFilter nextFilter = context.mock(NextFilter.class);
    private IoFilterChain filterChain = context.mock(IoFilterChain.class);

    @Test
    public void shouldIncreaseContentLength() throws Exception {
        final IoBuffer message = BUFFER_ALLOCATOR.wrap(BUFFER_ALLOCATOR.allocate(39));
        context.setThreadingPolicy(new Synchroniser());
        context.checking(new Expectations() { {
            oneOf(session).getWriteHeader("Content-Length"); will(returnValue("12"));
            oneOf(session).setWriteHeader("Content-Length", "51");

            oneOf(nextFilter).filterWrite(with(session), with(hasMessage(message)));

            oneOf(session).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).remove(with(any(HttpContentLengthAdjustmentFilter.class)));
        } });

        HttpContentLengthAdjustmentFilter filter = new HttpContentLengthAdjustmentFilter();
        filter.filterWrite(nextFilter, session, new DefaultWriteRequest(message));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldDefaultIncreaseContentLength() throws Exception {
        final IoBuffer message =  BUFFER_ALLOCATOR.wrap(BUFFER_ALLOCATOR.allocate(39));

        context.setThreadingPolicy(new Synchroniser());
        context.checking(new Expectations() { {
            oneOf(session).getWriteHeader("Content-Length"); will(returnValue(null));
            oneOf(session).setWriteHeader("Content-Length", "39");

            oneOf(nextFilter).filterWrite(with(session), with(hasMessage(message)));

            oneOf(session).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).remove(with(any(HttpContentLengthAdjustmentFilter.class)));
        } });

        HttpContentLengthAdjustmentFilter filter = new HttpContentLengthAdjustmentFilter();
        filter.filterWrite(nextFilter, session, new DefaultWriteRequest(message));

        context.assertIsSatisfied();
    }
}
