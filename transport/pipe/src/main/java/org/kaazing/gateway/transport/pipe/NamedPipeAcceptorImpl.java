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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultBindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.AbstractIoAcceptorEx;

public class NamedPipeAcceptorImpl extends AbstractIoAcceptorEx implements NamedPipeService {

    private final NamedPipeProcessor processor;
    private final ConcurrentMap<NamedPipeAddress, IoHandler> bindings;
    
    public NamedPipeAcceptorImpl() {
        this(null);
    }
    
    public NamedPipeAcceptorImpl(Executor executor) {
        super(new NamedPipeSessionConfig(), executor);
        
        this.processor = new NamedPipeProcessor();
        this.bindings = new ConcurrentHashMap<>();
    }
    
    @Override
    public TransportMetadata getTransportMetadata() {
        return NamedPipeSession.METADATA;
    }

    @Override
    public NamedPipeSessionConfig getSessionConfig() {
        return (NamedPipeSessionConfig) super.getSessionConfig();
    }

    @Override
    public NamedPipeAddress getLocalAddress() {
        return (NamedPipeAddress) super.getLocalAddress();
    }

    @Override
    public NamedPipeAddress getDefaultLocalAddress() {
        return (NamedPipeAddress) super.getDefaultLocalAddress();
    }

    public void setDefaultLocalAddress(NamedPipeAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    @Override
    public IoSession newSession(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        throw new UnsupportedOperationException("newSession");
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        unbind();
        return null;
    }

    @Override
    protected Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws IOException {
        Set<SocketAddress> newLocalAddresses = new HashSet<>();
        
        for (SocketAddress localAddress: localAddresses) {
            NamedPipeAddress localPipeAddress = (NamedPipeAddress)localAddress;
            if (localAddress == null) {
                throw new IOException("Named pipe local address cannot be null");
            }
            else if (localPipeAddress.isEphemeral()) {
                throw new IOException("Named pipe local address cannot be ephemeral");
            }
            else {
                boolean canBindSuccessfully = (bindings.get(localPipeAddress) == null);

                if (!canBindSuccessfully) {
                    throw new IOException(String.format("Named pipe address \"%s\" already bound", localPipeAddress));
                }
            }
            
            newLocalAddresses.add(localPipeAddress);
        }

        for (SocketAddress newLocalAddress: newLocalAddresses) {
            NamedPipeAddress newLocalPipeAddress = (NamedPipeAddress)newLocalAddress;

            boolean boundSuccessfully = (bindings.putIfAbsent(newLocalPipeAddress, getHandler()) == null);

            if (!boundSuccessfully) {
                bindings.keySet().removeAll(newLocalAddresses);
                throw new IOException(String.format("Named pipe address \"%s\" already bound", newLocalPipeAddress));
            }
        }
        
        return newLocalAddresses;
    }

    @Override
    protected BindFuture bindAsyncInternal(SocketAddress localAddress) {
	    DefaultBindFuture future = new DefaultBindFuture();
        try {
            bindInternal(Collections.singletonList(localAddress));
            future.setBound();
        }
        catch (IOException e) {
            future.setException(e);
        }
        return future;
    }

    @Override
    protected void unbind0(List<? extends SocketAddress> localAddresses)
            throws Exception {
        Set<NamedPipeAddress> bindingKeys = bindings.keySet();
        bindingKeys.removeAll(localAddresses);
    }
    
    @Override
    protected UnbindFuture unbindAsyncInternal(SocketAddress localAddress) {
        DefaultUnbindFuture future = new DefaultUnbindFuture();
        try {
            unbind0(Collections.singletonList(localAddress));
            future.setUnbound();
        }
        catch (Exception e) {
            future.setException(e);
        }
        return future;
    }

    NamedPipeSession newSession(NamedPipeAddress localAddress, NamedPipeSession remoteSession) throws Exception {
        
        IoHandler handler = bindings.get(localAddress);
        if (handler == null) {
            throw new IOException(String.format("Named pipe \"%s\" not bound", localAddress.getPipeName()));
        }
        
        NamedPipeSession session = new NamedPipeSession(this, processor, localAddress, handler);
        IoFilterChain chain = session.getFilterChain();
        IoFilterChainBuilder builder = getFilterChainBuilder();
        builder.buildFilterChain(chain);
        
        initSession(session, null, null);
        
        session.setRemoteSession(remoteSession);
        
        getListeners().fireSessionCreated(session);
        
        return session;
    }
}
