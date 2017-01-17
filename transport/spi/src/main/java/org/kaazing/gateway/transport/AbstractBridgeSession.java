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
package org.kaazing.gateway.transport;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;

public abstract class AbstractBridgeSession<S extends IoSessionEx, B extends IoBufferEx> extends AbstractIoSessionEx implements BridgeSession {

    private final IoProcessorEx<S> processor;
    private final IoSessionConfigEx sessionConfig;
    private final ResourceAddress localAddress;
    private final ResourceAddress remoteAddress;
    private final IoServiceEx service;

    private final TransportMetadata metadata;
    private final IoBufferAllocatorEx<? extends B> allocator;

    // thread safety
    private final AtomicReference<IoSessionEx> parent;
    private volatile Direction direction;
    private IoHandler handler;

    public AbstractBridgeSession(int parentIoLayer,
                                 Thread parentIoThread,
                                 Executor parentIoExecutor,
                                 IoServiceEx service, 
                                 IoProcessorEx<S> sIoProcessor, 
                                 ResourceAddress localAddress, 
                                 ResourceAddress remoteAddress, 
                                 IoBufferAllocatorEx<? extends B> allocator, 
                                 Direction direction) {
        this(parentIoLayer, parentIoThread, parentIoExecutor, service, sIoProcessor, localAddress, remoteAddress, null, allocator, direction, new DefaultIoSessionConfigEx());
    }

    public AbstractBridgeSession(int parentIoLayer,
                                 Thread parentIoThread,
                                 Executor parentIoExecutor,
                                 IoServiceEx service, 
                                 IoProcessorEx<S> sIoProcessor, 
                                 ResourceAddress localAddress, 
                                 ResourceAddress remoteAddress, 
                                 IoBufferAllocatorEx<? extends B> allocator, 
                                 Direction direction, 
                                 IoSessionConfigEx config) {
        // note: increment parent I/O layer
        this(parentIoLayer + 1, parentIoThread, parentIoExecutor, service, sIoProcessor, localAddress, remoteAddress, null, allocator, direction, config);

    }

    public AbstractBridgeSession(IoServiceEx service, 
                                 IoProcessorEx<S> processor, 
                                 ResourceAddress localAddress, 
                                 ResourceAddress remoteAddress, 
                                 IoSessionEx parent, 
                                 IoBufferAllocatorEx<? extends B> allocator, 
                                 Direction direction) {
        this(service, processor, localAddress, remoteAddress, parent, allocator, direction, new DefaultIoSessionConfigEx());
    }

    public AbstractBridgeSession(IoServiceEx service, 
                                 IoProcessorEx<S> processor, 
                                 ResourceAddress localAddress, 
                                 ResourceAddress remoteAddress, 
                                 IoSessionEx parent, 
                                 IoBufferAllocatorEx<? extends B> allocator, 
                                 Direction direction, 
                                 IoSessionConfigEx config) {
        // note: increment parent I/O layer
        this(checkParentNotNull(parent).getIoLayer() + 1, parent.getIoThread(), parent.getIoExecutor(), service, processor, localAddress, remoteAddress, parent, allocator, direction, config);

    }

    private AbstractBridgeSession(int ioLayer,
                                  Thread ioThread, 
                                  Executor ioExecutor, 
                                  IoServiceEx service, 
                                  IoProcessorEx<S> processor, 
                                  ResourceAddress localAddress, 
                                  ResourceAddress remoteAddress, 
                                  IoSessionEx parent, 
                                  IoBufferAllocatorEx<? extends B> allocator, 
                                  Direction direction, 
                                  IoSessionConfigEx config) {
        super(ioLayer, ioThread, ioExecutor, service.getThreadLocalWriteRequest(ioLayer));


        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        
        this.service = service;
        this.processor = processor;
        this.parent = new AtomicReference<>(parent);
        this.allocator = allocator;

        this.metadata = service.getTransportMetadata();
        this.handler = service.getHandler();

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;

        config.setAll(service.getSessionConfig());

        // KG-1466: only wrap config if it's an instance of DefaultSessionConfigEx, to avoid defeating downcast
        this.sessionConfig = config.getClass() == DefaultIoSessionConfigEx.class ? new BridgeSessionConfigEx(config, this.parent) : config;

        this.direction = direction;
    }

    @Override
    public final IoBufferAllocatorEx<? extends B> getBufferAllocator() {
        return allocator;
    }

	@Override
    public IoProcessorEx<S> getProcessor() {
        return processor;
    }

    @Override
    public IoSessionConfigEx getConfig() {
        return sessionConfig;
    }

    @Override
    public IoHandler getHandler() {
        return handler;
    }

    public void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
    }

    @Override
    public ResourceAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public ResourceAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public IoServiceEx getService() {
        return service;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
    	return metadata;
    }

    // --

    @Override
    public IoSessionEx getParent() {
    	return parent.get();
    }

    protected boolean compareAndSetParent(IoSessionEx expectedParent,
                                          IoSessionEx newParent) {
        return parent.compareAndSet(expectedParent, newParent);
    }

    protected IoSessionEx setParent(IoSessionEx newParent) {
        return parent.getAndSet(newParent);
    }

	@Override
	public Direction getDirection() {
		return direction;
	}

    /**
     * Behave similarly to connection reset by peer at NIO layer. This method should be called
     * from handlers' exceptionCaught method instead of calling fireExceptionCaught and
     * IoProcessor.remove, because the latter will fail if we're not on its IO thread.
     * @param cause
     */
    public void reset(final Throwable cause) {
        if (cause == null) {
            throw new NullPointerException("cause must not be null in AbstractBridgeSession.reset");
        }
        if (!isIoAligned() || getIoThread() == Thread.currentThread()) {
            reset0(cause);
        }
        else {
            getIoExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    reset0(cause);
                }

            });
        }
    }
    
    @Override
    protected void setIoAlignment0(Thread ioThread, Executor ioExecutor) {
        IoSessionEx parent = this.parent.get();
        if (parent != null) {
            parent.setIoAlignment(ioThread, ioExecutor);
        }
    }

    @Override
    protected void suspendRead1() {
        suspendRead2();

        IoSession parent = this.parent.get();
        if (parent != null) {
            parent.suspendRead();
        }
    }
    
    protected void suspendRead2() {
        super.suspendRead1();
    }
    
    @Override
    protected void resumeRead1() {
        // call super first to trigger processor.consume()
        resumeRead2();

        IoSession parent = this.parent.get();
        if (parent != null) {
            parent.resumeRead();
        }
    }

    protected void resumeRead2() {
        super.resumeRead1();
    }

    @SuppressWarnings("unchecked")
    private void reset0(Throwable cause) {
        try {
            getFilterChain().fireExceptionCaught(cause);
        }
        finally {
            if (!isClosing()) {
                getProcessor().remove((S) this);
            }
        }
    }

    /**
     * TODO: remove when SocksSession "upgrades" like HttpSession for WebSocket handshake (SocksSession
     * should eject itself from the transport layer hierarchy)
     */
    @Deprecated
    public void reset() {
        if (!isIoAligned() || getIoThread() == Thread.currentThread()) {
            reset0();
        }
        else {
            getIoExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    reset0();
                }

            });
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    private void reset0() {
        getProcessor().remove((S) this);
    }

	protected void setDirection(Direction direction) {
		this.direction = direction;
	}

    @Override
    public String toString() {

        String externalRemoteURI = getRemoteAddress().getExternalURI();
        String externalLocalURI = getLocalAddress().getExternalURI();
        if (getService() instanceof BridgeAcceptor) {
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(getIdAsString()).append(": ").append(getServiceName());
            sb.append(", server, ").append(externalRemoteURI).append(" => ");
            sb.append(externalLocalURI).append(')');
            String result = sb.toString();
            return result;
        } else {
            return "(" + getIdAsString() + ": " + getServiceName() + ", client, " +
                    externalLocalURI + " => " + externalRemoteURI + ')';
        }
    }

    // Taken from AbstractIoSession b/c i is marked private
    private String getIdAsString() {
        return String.format("#%08d", getId());
    }

    // Taken from AbstractIoSession b/c i is marked private
    private String getServiceName() {
        TransportMetadata tm = getTransportMetadata();
        if (tm == null) {
            return "null";
        } else {
            return tm.getProviderName() + ' ' + tm.getName();
        }
    }

    /**
     * This implementation of IoSessionConfig delegates set/getIdleTime methods to the parent session, if available.
     */
    private static class BridgeSessionConfigEx extends AbstractIoSessionConfigEx {
        private boolean propagateIdleTime = false;
        private final AtomicReference<IoSessionEx> parentReference;

        BridgeSessionConfigEx(IoSessionConfigEx wrappedConfig, AtomicReference<IoSessionEx> parentReference) {
            setAll(wrappedConfig);
            // set propagate to true now, as setAll() should not be propagated
            propagateIdleTime = true;
            this.parentReference = parentReference;
        }

        @Override
        public void setIdleTime(IdleStatus status, int idleTime) {
            // Don't allow setAll to overwrite idle time settings on the parent, grandparent, etc.
            if (propagateIdleTime) {
                IoSessionEx parent = parentReference.get();
                if (parent != null) {
                    parent.getConfig().setIdleTime(status, idleTime);
                }
                else {
                    super.setIdleTime(status, idleTime);
                }
            }
        }

        @Override
        public void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis) {
            // Don't allow setAll to overwrite idle time settings on the parent, grandparent, etc.
            if (propagateIdleTime) {
                IoSessionEx parent = parentReference.get();
                if (parent != null) {
                    IoSessionConfigEx config = parent.getConfig();
                    config.setIdleTimeInMillis(status, idleTimeMillis);
                }
            }
        }

        @Override
        public int getIdleTime(IdleStatus status) {
            IoSessionEx parent = parentReference.get();
            if (parent != null) {
                return parent.getConfig().getIdleTime(status);
            }
            else {
                return super.getIdleTime(status);
            }
        }

        @Override
        public long getIdleTimeInMillis(IdleStatus status) {
            IoSessionEx parent = parentReference.get();
            if (parent != null) {
                IoSessionConfigEx config = parent.getConfig();
                return config.getIdleTimeInMillis(status);
            }
            else {
            	return super.getIdleTimeInMillis(status);
            }
        }

        @Override
        protected void doSetAll(IoSessionConfigEx config) {
        }
    }

    private static IoSessionEx checkParentNotNull(IoSessionEx parent) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        return parent;
    }

}
