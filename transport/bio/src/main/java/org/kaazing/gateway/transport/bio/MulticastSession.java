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
package org.kaazing.gateway.transport.bio;

import java.net.MulticastSocket;
import java.net.SocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.IoSessionEx;

class MulticastSession extends AbstractIoSessionEx {

    static final TransportMetadata TRANSPORT_METADATA =
        new DefaultTransportMetadata(
                "bio", "multicast", true, false,
                MulticastAddress.class,
                DatagramSessionConfig.class, IoBuffer.class);

	private final IoServiceEx service;
	private final IoProcessorEx<MulticastSession> processor;
	private final MulticastSocket socket;
	private final SocketAddress localAddress;
	private final SocketAddress remoteAddress;
	private final DatagramSessionConfigEx config;
	private final IoBufferAllocatorEx<?> allocator;
	
	public MulticastSession(IoServiceEx service, IoProcessorEx<MulticastSession> processor, MulticastSocket socket, SocketAddress localAddress, SocketAddress remoteAddress) {
	    this(0, service, processor, socket, localAddress, remoteAddress);
	}
	
    public MulticastSession(int ioLayer, IoServiceEx service, IoProcessorEx<MulticastSession> processor, MulticastSocket socket, SocketAddress localAddress, SocketAddress remoteAddress) {
	    super(ioLayer, IoSessionEx.CURRENT_THREAD, IoSessionEx.IMMEDIATE_EXECUTOR, service.getThreadLocalWriteRequest(ioLayer));
		this.service = service;
		this.processor = processor;
		this.socket = socket;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.config = new DefaultDatagramSessionConfigEx();
		this.allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
	}
	
	@Override
    public IoBufferAllocatorEx<?> getBufferAllocator() {
        return allocator;
    }

    @Override
	public IoProcessorEx<MulticastSession> getProcessor() {
		return processor;
	}

	@Override
	public DatagramSessionConfigEx getConfig() {
		return config;
	}

	@Override
	public IoHandler getHandler() {
		return service.getHandler();
	}

	@Override
	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public IoServiceEx getService() {
		return service;
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

	public MulticastSocket getSocket() {
		return socket;
	}

}
