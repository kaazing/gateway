/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr;

import java.util.concurrent.Executor;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.extension.ActiveExtensions;
import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBuffer;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsrSession extends AbstractWsBridgeSession<WsrSession, WsrBuffer> {

    private static final CachingMessageEncoder WSR_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("wsr", encoder, message, allocator, flags);
        }

    };

    private int downstreamId;
    private int upstreamId;
    private ResourceAddress rtmpAddress;
    private final TimeoutCommand timeout = new TimeoutCommand(this);

    
    public WsrSession(IoServiceEx service, 
                      IoProcessorEx<WsrSession> processor, 
                      ResourceAddress localAddress,
                      ResourceAddress remoteAddress, 
                      IoSessionEx parent, 
                      IoBufferAllocatorEx<WsrBuffer> allocator, 
                      DefaultLoginResult loginResult, 
                      ActiveExtensions wsExtensions) {
        super(service, processor, localAddress, remoteAddress, parent, allocator, Direction.BOTH, loginResult, wsExtensions);
    }

    public WsrSession(int ioLayer, Thread parentIoThread, Executor parentIoExecutor, IoServiceEx service, IoProcessorEx<WsrSession> processor,
            ResourceAddress localAddress, ResourceAddress remoteAddress, IoBufferAllocatorEx<WsrBuffer> allocator,
            DefaultLoginResult loginResult, ActiveExtensions wsExtensions) {
        super(ioLayer, parentIoThread, parentIoExecutor, service, processor, localAddress, remoteAddress, allocator, Direction.BOTH,
                loginResult, wsExtensions);
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        return WSR_MESSAGE_ENCODER;
    }

    public void setUpstreamId(int upstreamId) {
        this.upstreamId = upstreamId;
    }

    public int getUpstreamId() {
        return upstreamId;
    }

    public void setDownstreamId(int downstreamId) {
        this.downstreamId = downstreamId;
    }

    public int getDownstreamId() {
        return downstreamId;
    }

    public Runnable getTimeoutCommand() {
        return timeout;
    }

    public void clearTimeoutCommand() {
        timeout.clear();
    }

    public boolean isSecure() {
        return rtmpAddress.toString().startsWith("rtmps");
    }

    @Override
    public IoSessionEx setParent(IoSessionEx newParent) {
    	return super.setParent(newParent);
    }

	public void setRtmpAddress(ResourceAddress rtmpAddress) {
		this.rtmpAddress = rtmpAddress;
	}

	public ResourceAddress getRtmpAddress() {
		return rtmpAddress;
	}

    // close session if reconnect timer elapses and no parent has been attached
    private static class TimeoutCommand implements Runnable {

        private volatile WsrSession session;

        public TimeoutCommand(WsrSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            WsrSession session = this.session;
            if (session != null) {
                // technically if this is being called then we have passed the timeout and no reconnect
                // has happened because it would have canceled this task, but doing a check just in case of a race condition
                if (!session.isClosing()) {
                    IoSession parent = session.getParent();
                    if (parent == null || parent.isClosing()) {
                        session.close(true);
                    }
                }
            }
            this.session = null; // avoid memory leak
        }

        public void clear() {
            session = null;
        }
    }

}
