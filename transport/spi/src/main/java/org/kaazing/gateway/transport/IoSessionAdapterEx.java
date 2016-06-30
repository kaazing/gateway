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

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public class IoSessionAdapterEx extends AbstractIoSessionEx {

    private volatile IoServiceEx service;

    private volatile IoSessionConfigEx config = new AbstractIoSessionConfigEx() {
        @Override
        protected void doSetAll(IoSessionConfigEx config) {
            // Do nothing
        }
    };

    private final IoProcessorEx<? extends IoSessionEx> processor;

    private volatile IoHandler handler;
    private volatile SocketAddress localAddress;
    private volatile SocketAddress remoteAddress;
    private volatile TransportMetadata transportMetadata;

    /**
     * Creates a new instance.
     */
    public <T extends IoSessionEx> IoSessionAdapterEx(Thread ioThread, Executor ioExecutor, IoServiceEx service, IoProcessorEx<T> processor,
            IoSessionDataStructureFactory factory) {
        super(0, ioThread, ioExecutor, new ShareableWriteRequest());
        this.service = service;
        this.processor = processor;

        try {
            setAttributeMap(factory.getAttributeMap(this));
            setWriteRequestQueue(factory.getWriteRequestQueue(this));
        } catch (Exception e) {
            throw new InternalError();
        }
    }

    @Override
    public IoBufferAllocatorEx<?> getBufferAllocator() {
        return SimpleBufferAllocator.BUFFER_ALLOCATOR;
    }

    @Override
    public IoSessionConfigEx getConfig() {
        return config;
    }

    /**
     * Sets the configuration of this session.
     */
    public void setConfig(IoSessionConfigEx config) {
        if (config == null) {
            throw new NullPointerException("config");
        }

        this.config = config;
    }

    @Override
    public IoHandler getHandler() {
        return handler;
    }

    /**
     * Sets the {@link IoHandler} which handles this session.
     */
    public void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        this.handler = handler;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the socket address of local machine which is associated with
     * this session.
     */
    public void setLocalAddress(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        this.localAddress = localAddress;
    }

    /**
     * Sets the socket address of remote peer.
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        this.remoteAddress = remoteAddress;
    }

    @Override
    public IoServiceEx getService() {
        return service;
    }

    /**
     * Sets the {@link IoService} which provides I/O service to this session.
     */
    public void setService(IoServiceEx service) {
        if (service == null) {
            throw new NullPointerException("service");
        }

        this.service = service;
    }

    @Override
    public final IoProcessorEx<? extends IoSessionEx> getProcessor() {
        return processor;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return transportMetadata;
    }

    /**
     * Sets the {@link TransportMetadata} that this session runs on.
     */
    public void setTransportMetadata(TransportMetadata transportMetadata) {
        if (transportMetadata == null) {
            throw new NullPointerException("transportMetadata");
        }

        this.transportMetadata = transportMetadata;
    }

    @Override
    public void setScheduledWriteBytes(int byteCount){
        super.setScheduledWriteBytes(byteCount);
    }


    /**
     * Update all statistical properties related with throughput.  By default
     * this method returns silently without updating the throughput properties
     * if they were calculated already within last
     * {@link IoSessionConfig#getThroughputCalculationInterval() calculation interval}.
     * If, however, <tt>force</tt> is specified as <tt>true</tt>, this method
     * updates the throughput properties immediately.
     */
    public void updateThroughput(boolean force) {
        super.updateThroughput(System.currentTimeMillis(), force);
    }
}
