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
package org.apache.mina.transport.vmpipe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListenerSupport;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequestQueue;

/**
 * A {@link IoSession} for in-VM transport (VM_PIPE).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class VmPipeSession extends AbstractIoSession {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "mina", "vmpipe", false, false,
                    VmPipeAddress.class,
                    VmPipeSessionConfig.class,
                    Object.class);

    private static final VmPipeSessionConfig CONFIG = new DefaultVmPipeSessionConfig();

    private final IoService service;

    private final IoServiceListenerSupport serviceListeners;

    private final VmPipeAddress localAddress;

    private final VmPipeAddress remoteAddress;

    private final VmPipeAddress serviceAddress;

    private final IoHandler handler;

    private final VmPipeFilterChain filterChain;

    private final VmPipeSession remoteSession;

    private final Lock lock;

    final BlockingQueue<Object> receivedMessageQueue;

    /*
     * Constructor for client-side session.
     */
    VmPipeSession(IoService service,
                      IoServiceListenerSupport serviceListeners,
                      VmPipeAddress localAddress, IoHandler handler, VmPipe remoteEntry) {
        this.service = service;
        this.serviceListeners = serviceListeners;
        lock = new ReentrantLock();
        this.localAddress = localAddress;
        remoteAddress = serviceAddress = remoteEntry.getAddress();
        this.handler = handler;
        filterChain = new VmPipeFilterChain(this);
        receivedMessageQueue = new LinkedBlockingQueue<>();

        remoteSession = new VmPipeSession(this, remoteEntry);
    }

    /*
     * Constructor for server-side session.
     */
    private VmPipeSession(VmPipeSession remoteSession, VmPipe entry) {
        service = entry.getAcceptor();
        serviceListeners = entry.getListeners();
        lock = remoteSession.lock;
        localAddress = serviceAddress = remoteSession.remoteAddress;
        remoteAddress = remoteSession.localAddress;
        handler = entry.getHandler();
        filterChain = new VmPipeFilterChain(this);
        this.remoteSession = remoteSession;
        receivedMessageQueue = new LinkedBlockingQueue<>();
    }

    public IoService getService() {
        return service;
    }

    @Override
    public IoProcessor<VmPipeSession> getProcessor() {
        return filterChain.getProcessor();
    }

    IoServiceListenerSupport getServiceListeners() {
        return serviceListeners;
    }

    public VmPipeSessionConfig getConfig() {
        return CONFIG;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public VmPipeSession getRemoteSession() {
        return remoteSession;
    }

    public IoHandler getHandler() {
        return handler;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    public VmPipeAddress getRemoteAddress() {
        return remoteAddress;
    }

    public VmPipeAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public VmPipeAddress getServiceAddress() {
        return serviceAddress;
    }

    void increaseWrittenBytes0(int increment, long currentTime) {
        super.increaseWrittenBytes(increment, currentTime);
    }

    WriteRequestQueue getWriteRequestQueue0() {
        return super.getWriteRequestQueue();
    }

    Lock getLock() {
        return lock;
    }
}
