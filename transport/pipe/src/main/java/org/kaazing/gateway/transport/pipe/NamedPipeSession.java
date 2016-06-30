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
package org.kaazing.gateway.transport.pipe;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.AbstractIoSessionEx;

public class NamedPipeSession extends AbstractIoSessionEx {

    // Note: although the pipe transport does not actually fragment the buffers
    //       it does propagate raw buffers that may already be fragmented
    //       so decoders will still need to deal with potentially fragmented input
    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "kaazing", "pipe", false, true,
                    NamedPipeAddress.class,
                    NamedPipeSessionConfig.class,
                    Object.class);

    private final NamedPipeService service;
    private final NamedPipeProcessor processor;
    private final NamedPipeAddress localAddress;
    private final IoHandler handler;
    private final NamedPipeSessionConfig config;
    private final AtomicReference<NamedPipeSession> remoteSession;
    private final AtomicBoolean closingOnFlush;
    private final AtomicBoolean flushingInternal;


    private final IoFutureListener<CloseFuture> closeOnFlush = new IoFutureListener<CloseFuture>() {
        @Override
        public void operationComplete(CloseFuture future) {
            closingOnFlush.set(true);
            NamedPipeSession.this.close(false);
        }
    };
    
    public NamedPipeSession(NamedPipeService service, NamedPipeProcessor processor, NamedPipeAddress localAddress, IoHandler handler) {
	    super(0, CURRENT_THREAD, IMMEDIATE_EXECUTOR, service.getThreadLocalWriteRequest(0));
        this.service = service;
        this.processor = processor;
        this.localAddress = localAddress;
        this.handler = handler;
        
        this.config = new NamedPipeSessionConfig();
        this.remoteSession = new AtomicReference<>();
        this.closingOnFlush = new AtomicBoolean();
        this.flushingInternal = new AtomicBoolean();
    }

    @Override
    public IoBufferAllocatorEx<?> getBufferAllocator() {
	    // TODO: consider overhead of ThreadLocal shared here
        return SimpleBufferAllocator.BUFFER_ALLOCATOR;
    }

    @Override
    public NamedPipeService getService() {
        return service;
    }

    @Override
    public IoHandler getHandler() {
        return handler;
    }

    @Override
    public NamedPipeSessionConfig getConfig() {
        return config;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    @Override
    public NamedPipeAddress getRemoteAddress() {
        NamedPipeSession remoteSession = this.remoteSession.get();
        return (remoteSession != null) ? remoteSession.getLocalAddress() : null;
    }

    @Override
    public NamedPipeAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public NamedPipeProcessor getProcessor() {
        return processor;
    }

    NamedPipeSession getRemoteSession() {
        return remoteSession.get();
    }
    
    void setRemoteSession(NamedPipeSession remoteSession) {
        if (remoteSession != null) {
            if (!this.remoteSession.compareAndSet(null, remoteSession)) {
                throw new NamedPipeException(String.format("Named pipe \"%s\" already attached to remote session", localAddress.getPipeName()));
            }
            if (!isClosing()) {
                getProcessor().updateTrafficControl(this);
            }
            remoteSession.getCloseFuture().addListener(closeOnFlush);
        }
        else {
            NamedPipeSession oldRemoteSession = this.remoteSession.getAndSet(null);
            if (oldRemoteSession != null) {
                oldRemoteSession.getCloseFuture().removeListener(closeOnFlush);

                // recursive call to detach peer (unless already done earlier in call stack)
                oldRemoteSession.setRemoteSession(null);
                
                if (!oldRemoteSession.isClosing() && !oldRemoteSession.isClosingOnFlush()) {
                    oldRemoteSession.getProcessor().remove(oldRemoteSession);
                }
            }
        }
    }

    private boolean isClosingOnFlush() {
        return closingOnFlush.get();
    }

    boolean setFlushInternalStarted() {
        return flushingInternal.compareAndSet(false, true);
    }

    boolean setFlushInternalComplete() {
        return flushingInternal.compareAndSet(true, false);
    }

    private final AtomicInteger flushCount = new AtomicInteger();
    
    boolean beginFlush() {
        return (flushCount.getAndIncrement() == 0);
    }

    boolean endFlush() {
        return (flushCount.decrementAndGet() == 0);
    }
}
