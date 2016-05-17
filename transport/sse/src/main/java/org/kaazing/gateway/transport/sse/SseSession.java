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

import java.util.concurrent.Executor;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class SseSession extends AbstractBridgeSession<SseSession, SseBuffer> {

    private static final CachingMessageEncoder SSE_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("sse", encoder, message, allocator, flags);
        }

    };

    // Setting the 'parent' member variable in the super class results in SSE IT tests to fail. Temporarily use
    // a different member variable to hold the parent session. This is needed for an extension-service built
    // for a customer.
    private final IoSessionEx parentSession;

    public SseSession(IoServiceEx service, IoProcessorEx<SseSession> processor, ResourceAddress localAddress, ResourceAddress remoteAddress, IoSessionEx parent,
                      IoBufferAllocatorEx<SseBuffer> allocator) {
    	super(service, processor, localAddress, remoteAddress, parent, allocator, Direction.WRITE, new DefaultSseSessionConfig());
        this.parentSession = parent;
    }

    public SseSession(int ioLayer,
                      Thread ioThread,
                      Executor ioExecutor,
                      IoServiceEx service,
                      IoProcessorEx<SseSession> processor,
                      ResourceAddress localAddress,
                      ResourceAddress remoteAddress,
                      IoBufferAllocatorEx<SseBuffer> allocator,
                      IoSessionEx parent) {
        super(ioLayer, ioThread, ioExecutor, service, processor, localAddress, remoteAddress, allocator, Direction.WRITE, new DefaultSseSessionConfig());
        this.parentSession = parent;
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        return SSE_MESSAGE_ENCODER;
    }

    @Override
	public SseSessionConfig getConfig() {
		return (DefaultSseSessionConfig)super.getConfig();
	}

	public void attach(final IoSessionEx newParent) {
	    if (!isClosing()) {
	        IoSession oldParent = setParent(newParent);
	        if (!isWriteSuspended()) {
	            getProcessor().flush(this);
	        }

	        // close the old HttpSession parent, letting it complete normally,
	    	// if this is an overlapping attach replacing an existing downstream
			if (oldParent != null) {
				newParent.suspendWrite();
				// use immediate = false to place CLOSE_REQUEST in suspended write queue
				// allowing any number of overlapping requests to chain successfully
				oldParent.close(false).addListener(new IoFutureListener<CloseFuture>() {
					@Override
					public void operationComplete(CloseFuture future) {
						newParent.resumeWrite();
					}
				});
			}
	    }
	    else {
	        if (newParent != null) {
	            newParent.close(false);
	        }
	    }
	}
	
	public boolean detach(IoSessionEx oldParent) {
		return compareAndSetParent(oldParent, null);
	}
	
    @Override
    public ResourceAddress getLocalAddress() {
        return super.getLocalAddress();
    }
    
    @Override
    public ResourceAddress getRemoteAddress() {
        return super.getRemoteAddress();
    }

	@Override
	protected IoSessionEx setParent(IoSessionEx parent) {
		return super.setParent(parent);
	}

    // Temporary workaround till issues related to setting the 'parent' member variable
    // in the super class are addressed. This method is used in an extension-service
    // built for a customer
    public IoSessionEx parent() {
        return parentSession;
    }
}
