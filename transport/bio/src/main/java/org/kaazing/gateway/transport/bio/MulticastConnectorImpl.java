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

import static java.lang.String.format;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;
import org.apache.mina.util.ConcurrentHashSet;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.service.AbstractIoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.util.ExceptionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulticastConnectorImpl extends AbstractIoConnectorEx {

    private final Logger logger = LoggerFactory.getLogger("transport.mcp");
    
    private final ConcurrentMap<SocketAddress, Handle> boundHandles;
	private final MulticastProcessor processor;
	private final AtomicInteger nextId;

    private ResourceAddressFactory resourceAddressFactory;
	
    public MulticastConnectorImpl(ResourceAddressFactory resourceAddressFactory) {
        super(new DefaultDatagramSessionConfigEx(), null);
        this.resourceAddressFactory = resourceAddressFactory;
        processor = new MulticastProcessor();
		boundHandles = new ConcurrentHashMap<>();
		this.nextId = new AtomicInteger();
	}

	@Override
	protected ConnectFuture connect0(SocketAddress remoteAddress,
			SocketAddress localAddress,
			IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {

		if (localAddress != null) {
			return DefaultConnectFuture.newFailedFuture(new IllegalArgumentException("localAddress is not null").fillInStackTrace());
		}
		
		try {
			assert (localAddress == null);

			MulticastAddress remoteMulticastAddress = (MulticastAddress)remoteAddress;
			Handle newHandle = new Handle(remoteMulticastAddress);
			Handle handle = boundHandles.putIfAbsent(remoteMulticastAddress, newHandle);
			
			if (handle == null) {
				handle = newHandle;
				handle.joinGroup();
			}
			
			// generate unique identifier for client session
			InetAddress groupAddress = remoteMulticastAddress.getGroupAddress();
			NetworkInterface device = remoteMulticastAddress.getDevice();
			int bindPort = remoteMulticastAddress.getBindPort();
			int uniqueId = nextId.getAndIncrement();
			MulticastAddress localMulticastAddress = new MulticastAddress(groupAddress, device, bindPort, uniqueId);

			return handle.connect(localMulticastAddress, sessionInitializer);
		}
		catch (IOException e) {
			logger.error("Unable to connect to resource: " + localAddress + " cause: " + e.getMessage(), e);
			return DefaultConnectFuture.newFailedFuture(e);
		}
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		// leave all the multicast groups
		for (Handle handle : boundHandles.values()) {
			handle.leaveGroup();
		}
		
		return null;
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return MulticastSession.TRANSPORT_METADATA;
	}

    @Override
    public DatagramSessionConfigEx getSessionConfig() {
        return (DatagramSessionConfigEx) super.getSessionConfig();
    }

	private class Handle implements Runnable {

		private final MulticastSocket socket;
		private final MulticastAddress remoteAddress;
		private final InetSocketAddress groupAddress;
		private final NetworkInterface device;
		private final Set<IoSessionEx> dispatchSessions;
		
		public Handle(MulticastAddress remoteAddress) throws IOException {
			this.remoteAddress = remoteAddress;
			this.socket = new MulticastSocket(new InetSocketAddress(remoteAddress.getGroupAddress(), remoteAddress.getBindPort()));
			this.groupAddress = new InetSocketAddress(remoteAddress.getGroupAddress(), 0);
			this.device = remoteAddress.getDevice();
			this.dispatchSessions = new ConcurrentHashSet<>();
		}

		@Override
		public void run() {
			while (socket.isBound()) {
				try {
					// blocking read or exception on close
					byte[] buf = new byte[socket.getReceiveBufferSize()];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					if (!dispatchSessions.isEmpty()) {
						// prepare message
						byte[] data = packet.getData();
						int offset = packet.getOffset();
						int length = packet.getLength();
						ByteBuffer message = ByteBuffer.wrap(data, offset, length);
	
						for (IoSessionEx dispatchSession : dispatchSessions) {
							// verify session can receive messages
							if (!dispatchSession.isReadSuspended()) {
								// deliver message
							    IoBufferAllocatorEx<?> allocator = dispatchSession.getBufferAllocator();
								dispatchSession.getFilterChain().fireMessageReceived(allocator.wrap(message.duplicate()));
							}
						}
					}
				} 
				catch (IOException e) {
					if (socket.isClosed()) {
						// socket closed, so close sessions for this local address
						for (IoSession dispatchSession : dispatchSessions) {
							if (remoteAddress.equals(dispatchSession.getRemoteAddress())) {
								dispatchSession.close(true);
							}
						}

						Handle h = boundHandles.remove(remoteAddress);
						if (h != this) {
							throw new IllegalStateException("Duplicate local address binding");
						}
					}
					break;
				}
			}
		}

		public ConnectFuture connect(SocketAddress localAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
            MulticastAddress multicastAddress = (MulticastAddress) localAddress;
            String uri = format("udp://%s:%d", multicastAddress.getGroupAddress().getHostAddress(),
                    multicastAddress.getBindPort());
            ResourceAddress resourceAddress = resourceAddressFactory.newResourceAddress(uri);
            final MulticastSession newSession = new MulticastSession(MulticastConnectorImpl.this, processor, socket,
                    resourceAddress, remoteAddress);

            DefaultConnectFuture connectFuture = new DefaultConnectFuture();
            initSession(newSession, connectFuture, sessionInitializer);
            newSession.getProcessor().add(newSession);
            dispatchSessions.add(newSession);
            newSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
				@Override
				public void operationComplete(CloseFuture future) {
					dispatchSessions.remove(newSession);
				}
			});
            
            return connectFuture;
		}
		
		public void joinGroup() throws IOException {
			socket.joinGroup(groupAddress, device);
			executeWorker(this);
		}
		
		public void leaveGroup() throws IOException {
			if (!socket.isClosed()) {
				socket.leaveGroup(groupAddress, device);
				socket.close();
			}
		}
	}
	
	private class MulticastProcessor implements IoProcessorEx<MulticastSession> {

		@Override
		public void dispose() {
		}

		@Override
		public boolean isDisposed() {
			return false;
		}

		@Override
		public boolean isDisposing() {
			return false;
		}

		@Override
		public void add(MulticastSession session) {
			boolean notified = false;
			
	        try {
	            // Build the filter chain of this session.
	            session.getService().getFilterChainBuilder().buildFilterChain(
	                    session.getFilterChain());

	            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
	            // in AbstractIoFilterChain.fireSessionOpened().
	            session.getService().getListeners()
	                    .fireSessionCreated(session);
	            notified = true;
	        } catch (Throwable e) {
	            if (notified) {
	                // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
	                // and call ConnectFuture.setException().
	                session.close(true);
	                
	                IoFilterChain filterChain = session.getFilterChain();
	                filterChain.fireExceptionCaught(e);
	            } else {
	                ExceptionMonitor.getInstance().exceptionCaught(e, session);
	            }
	        }
		}
		
		@Override
		public void flush(MulticastSession session) {
			// TODO: for multicast writes
		}
		
		@Override
		public void remove(MulticastSession session) {
	        getListeners().fireSessionDestroyed(session);
		}

		@Override
		public void updateTrafficControl(MulticastSession session) {
		}

	}
}
