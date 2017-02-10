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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultBindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.AbstractIoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.util.ExceptionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulticastAcceptorImpl extends AbstractIoAcceptorEx {

    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();
    
    private final Logger logger = LoggerFactory.getLogger("transport.mcp");
    
    private final ConcurrentMap<SocketAddress, Handle> boundHandles;
	private final MulticastProcessor processor;
	
    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    public MulticastAcceptorImpl() {
    	this(new DefaultDatagramSessionConfigEx(), null);
    }
    
    public MulticastAcceptorImpl(IoSessionConfigEx sessionConfig) {
    	this(sessionConfig, null);
    }
    
    public MulticastAcceptorImpl(IoSessionConfigEx sessionConfig, Executor executor) {
		super(sessionConfig, executor);
		
		boundHandles = new ConcurrentHashMap<>();
		processor = new MulticastProcessor();
	}

    @Override
    public DatagramSessionConfigEx getSessionConfig() {
        return (DatagramSessionConfigEx) super.getSessionConfig();
    }

	@Override
    protected BindFuture bindAsyncInternal(SocketAddress localAddress) {
	    List<? extends SocketAddress> localAddresses = Collections.singletonList(localAddress);
        try {
            bindInternal(localAddresses);
            return DefaultBindFuture.succeededFuture();
        } catch (Exception e) {
            DefaultBindFuture bindFuture = new DefaultBindFuture();
            bindFuture.setException(e);
            return bindFuture;
        }
    }

    @Override
    protected UnbindFuture unbindAsyncInternal(SocketAddress localAddress) {
        List<? extends SocketAddress> localAddresses = Collections.singletonList(localAddress);
        try {
            unbind0(localAddresses);
            return DefaultUnbindFuture.succeededFuture();
        } catch (Exception e) {
            DefaultUnbindFuture unbindFuture = new DefaultUnbindFuture();
            unbindFuture.setException(e);
            return unbindFuture;
        }
    }

    @Override
	protected Set<SocketAddress> bindInternal(
			List<? extends SocketAddress> localAddresses) throws Exception {
		
		Set<SocketAddress> boundAddresses = new HashSet<>();
        List<SocketAddress> failedAddresses = new LinkedList<>();
		
		for (SocketAddress localAddress : localAddresses) {
			MulticastAddress multicastAddress = (MulticastAddress)localAddress;
			Handle handle = new Handle(multicastAddress);
			Handle oldHandle = boundHandles.putIfAbsent(localAddress, handle);
			if (oldHandle != null) {
            	failedAddresses.add(localAddress);
			}
			else {
	            try {
	            	handle.bind();
	                logger.debug("Bound to resource: " + localAddress);
	            }
	            catch (IOException e) {
	                String error = "Unable to bind to resource: " + localAddress + " cause: " + e.getMessage();
	                logger.error(error);
	                throw new RuntimeException(error);
	            }
				boundAddresses.add(multicastAddress);
			}
		}
		
        if (!failedAddresses.isEmpty()) {
            throw new RuntimeException("Addresses already bound to different handlers: " + failedAddresses);
        }
		
		return boundAddresses;
	}

	@Override
	protected void unbind0(List<? extends SocketAddress> localAddresses)
			throws Exception {
		
		for (SocketAddress localAddress : localAddresses) {
			Handle handle = boundHandles.get(localAddress);
			if (handle != null) {
	            try {
	            	handle.unbind();
	                logger.debug("Unbound from resource: " + localAddress);
	            }
	            catch (IOException e) {
	                String error = "Unable to unbind from resource: " + localAddress + " cause: " + e.getMessage();
	                logger.error(error);
	                throw new RuntimeException(error);
	            }
			}
		}
	}

	// TODO: change to return void for 2.0.0-RCx upgrade
	@Override
	protected IoFuture dispose0() throws Exception {
		
		// close all recycled sessions that have not yet timed out
		// this triggers the sessionClosed event for the filter chain
		Set<IoSession> managedSessions = new HashSet<>(getManagedSessions().values());
		for (IoSession managedSession : managedSessions) {
			managedSession.close(true);
		}
		
		// unbind handles that may not yet have an associated session
		for (Handle handle : boundHandles.values()) {
			try {
				handle.unbind();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// TODO: remove return for 2.0.0-RCx upgrade
		return null;
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return MulticastSession.TRANSPORT_METADATA;
	}

	@Override
    public final IoSessionEx newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        synchronized (bindLock) {
            if (!isActive()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
            }

            try {
                return newSessionWithoutLock(remoteAddress, localAddress);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeIoException("Failed to create a session.", e);
            }
        }
    }

    private IoSessionEx newSessionWithoutLock(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        Handle handle = boundHandles.get(localAddress);
        if (handle == null) {
            throw new IllegalArgumentException("Unknown local address: " + localAddress);
        }

        IoSessionEx session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
		synchronized (sessionRecycler) {
            session = (IoSessionEx) sessionRecycler.recycle(localAddress, remoteAddress);
            if (session != null && !session.isClosing()) {
                return session;
            }

            // If a new session needs to be created.
            MulticastSession newSession = new MulticastSession(this, processor, handle.socket, handle.localAddress, remoteAddress);
            sessionRecycler.put(newSession);
            session = newSession;
        }

		initSession(session, null, null);

        try {
            this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
            getListeners().fireSessionCreated(session);
        }
        catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t, session);
        }

        return session;
    }
	
	public IoSessionRecycler getSessionRecycler() {
		return sessionRecycler;
	}

	private class Handle implements Runnable {

		private final MulticastSocket socket;
		private final SocketAddress localAddress;
		private final InetSocketAddress groupAddress;
		private final NetworkInterface device;
		
		public Handle(MulticastAddress localAddress) throws IOException {
			this.localAddress = localAddress;
			this.socket = new MulticastSocket(new InetSocketAddress(localAddress.getGroupAddress(), localAddress.getBindPort()));
			this.groupAddress = new InetSocketAddress(localAddress.getGroupAddress(), 0);
			this.device = localAddress.getDevice();
		}

		@Override
		public void run() {
			while (socket.isBound()) {
				try {
					// blocking read or exception on close
					byte[] buf = new byte[socket.getReceiveBufferSize()];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					// recycle session if necessary
					SocketAddress remoteAddress = packet.getSocketAddress();
					IoSessionEx session = newSession(remoteAddress, localAddress);

					// verify session can receive messages
					if (!session.isReadSuspended()) {
						// prepare message
						byte[] data = packet.getData();
						int offset = packet.getOffset();
						int length = packet.getLength();
						IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                        IoBufferEx message = allocator.wrap(ByteBuffer.wrap(data, offset, length));
						
						// deliver message
						session.getFilterChain().fireMessageReceived(message);
					}
				} 
				catch (IOException e) {
					if (socket.isClosed()) {
						// socket closed, so close sessions for this local address
						Set<IoSession> managedSessions = new HashSet<>(getManagedSessions().values());
						for (IoSession managedSession : managedSessions) {
							if (localAddress.equals(managedSession.getLocalAddress())) {
								managedSession.close(true);
							}
						}

						Handle h = boundHandles.remove(localAddress);
						if (h != this) {
							throw new IllegalStateException("Duplicate local address binding");
						}
					}
					break;
				}
			}
		}
		
		public void bind() throws IOException {
			socket.joinGroup(groupAddress, device);
			executeWorker(this);
		}
		
		public void unbind() throws IOException {
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
		}
		
		@Override
		public void flush(MulticastSession session) {
			// TODO: for multicast writes
		}
		
		@Override
		public void remove(MulticastSession session) {
			session.getSocket().close();
			
	        getSessionRecycler().remove(session);
	        getListeners().fireSessionDestroyed(session);
		}

		@Override
		public void updateTrafficControl(MulticastSession session) {
		}

	}
}
