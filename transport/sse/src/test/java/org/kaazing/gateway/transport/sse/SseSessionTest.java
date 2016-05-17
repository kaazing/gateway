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
package org.kaazing.gateway.transport.sse;

import static java.lang.Thread.currentThread;
import static org.kaazing.mina.core.session.IoSessionEx.IMMEDIATE_EXECUTOR;

import java.net.URISyntaxException;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public class SseSessionTest {

    @Test 
    @SuppressWarnings("unchecked")
	public void testGetConfig() throws URISyntaxException {
	    Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

		final IoServiceEx service = context.mock(IoServiceEx.class);
        final IoProcessorEx<SseSession> processor = (IoProcessorEx<SseSession>)context.mock(IoProcessorEx.class);
		final IoSessionEx parent = context.mock(IoSessionEx.class);
		final IoBufferAllocatorEx<SseBuffer> allocator = context.mock(IoBufferAllocatorEx.class);
		final IoHandler handler = context.mock(IoHandler.class);
				
		context.checking(new Expectations() {
			{
				try {
					allowing(service).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(SseProtocol.NAME)));
					allowing(service).getHandler(); will(returnValue(handler));
					allowing(service).getSessionConfig(); will(returnValue(new DefaultSseSessionConfig()));
                    allowing(service).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(parent).getIoLayer(); will(returnValue(0));
                    allowing(parent).getIoThread(); will(returnValue(currentThread()));
                    allowing(parent).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
				} 
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		
        ResourceAddress localAddress = ResourceAddressFactory.newResourceAddressFactory().newResourceAddress("sse://localhost:8000/sse");
        ResourceAddress remoteAddress = ResourceAddressFactory.newResourceAddressFactory().newResourceAddress("sse://localhost:8000/sse");
		SseSession session = new SseSession(service, processor, localAddress, remoteAddress, parent, allocator);
		session.getConfig(); // Was causing ClassCastException (KG-1466)
		
		context.assertIsSatisfied();
	}
}
