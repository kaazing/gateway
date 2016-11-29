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
package org.kaazing.gateway.service.proxy;

import java.util.Collection;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public abstract class AbstractProxyAcceptHandler extends AbstractProxyHandler {

    private AbstractProxyHandler connectHandler;
    private Collection<String> connectURIs;
    private ServiceConnectManager serviceConnectManager = null;

    protected AbstractProxyAcceptHandler() {
        connectHandler = createConnectHandler();
    }

    protected abstract AbstractProxyHandler createConnectHandler();

    protected AbstractProxyHandler getConnectHandler() {
        return connectHandler;
    }

    protected Collection<String> getConnectURIs() {
        return connectURIs;
    }

    public void setConnectURIs(Collection<String> connectURIs) {
        this.connectURIs = connectURIs;
    }

    // FIXME:  For the sake of management, how should this data be exposed?  For now there is access to the service connect manager,
    //         put perhaps management can attach some kind of listener that gets updated with the right info?
    public ServiceConnectManager getServiceConnectManager() {
        return serviceConnectManager;
    }

    public void initServiceConnectManager(BridgeServiceFactory bridgeServiceFactory) {
        String connectURI = connectURIs.iterator().next();
        serviceConnectManager = new ServiceConnectManager(getServiceContext(), getConnectHandler(),
                bridgeServiceFactory, connectURI, getMaximumRecoveryInterval(), getPreparedConnectionCount());
    }

    public void startServiceConnectManager() {
        serviceConnectManager.start();
    }

    public ConnectFuture getNextConnectFuture(final IoSessionInitializer<ConnectFuture> connectInitializer) {
        return serviceConnectManager.getNextConnectFuture(connectInitializer);
    }

    @Override
    public void sessionCreated(IoSession session)
    {
        super.sessionCreated(session);

        if (isDeferredConnectStrategy()) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.addLast("proxy#deferred", new DeferredConnectStrategyFilter());
        }
    }

    @Override
    public void sessionOpened(IoSession session) {
        // guarantee strongly-typed buffers; this is the accept-side so the
        // service is not a client.
        initFilterChain(session, false);
    }

    protected void initFilterChain(IoSession session, boolean client) {
        IoFilterChain filterChain = session.getFilterChain();
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        filterChain.addLast("duplicate", new DuplicateBufferFilter(allocator));
    }

    @Override
    public void setMaximumPendingBytes(int maximumPendingBytes) {
        super.setMaximumPendingBytes(maximumPendingBytes);
        connectHandler.setMaximumPendingBytes(maximumPendingBytes);
    }
}
