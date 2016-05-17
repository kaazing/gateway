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

import static org.apache.mina.core.future.DefaultConnectFuture.newFailedFuture;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.service.AbstractIoConnectorEx;

public class NamedPipeConnectorImpl extends AbstractIoConnectorEx implements NamedPipeService {

	private final NamedPipeProcessor processor;
	private volatile Ref<NamedPipeAcceptorImpl> acceptorRef;
	
	public NamedPipeConnectorImpl() {
		this(null);
	}
	
	public NamedPipeConnectorImpl(Executor executor) {
		super(new NamedPipeSessionConfig(), executor);
		this.processor = new NamedPipeProcessor();
	}
	
	public void setNamedPipeAcceptor(Ref<NamedPipeAcceptorImpl> acceptorRef) {
		this.acceptorRef = acceptorRef;
	}

	void setNamedPipeAcceptor(final NamedPipeAcceptorImpl acceptor) {
		setNamedPipeAcceptor(new Ref<NamedPipeAcceptorImpl>() {
			@Override
			public NamedPipeAcceptorImpl get() {
				return acceptor;
			}
		});
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return NamedPipeSession.METADATA;
	}

	@Override
	protected ConnectFuture connect0(SocketAddress remoteAddress,
			SocketAddress localAddress,
			IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {

		NamedPipeAcceptorImpl acceptor = (acceptorRef != null) ? acceptorRef.get() : null;
		
        if (acceptor == null) {
        	return newFailedFuture(new NamedPipeException("NamedPipeAcceptor not available"));
        }

        DefaultConnectFuture future = new DefaultConnectFuture();

        NamedPipeAddress remotePipeAddress = (NamedPipeAddress)remoteAddress;
        String remotePipeName = remotePipeAddress.getPipeName();
        
        NamedPipeAddress localPipeAddress = (NamedPipeAddress)localAddress;
        if (localPipeAddress == null) {
            localPipeAddress = new NamedPipeAddress(remotePipeName, true);
        }
        
        if (!localPipeAddress.getPipeName().equals(remotePipeName)) {
        	return newFailedFuture(new NamedPipeException("Local NamedPipeAddress pipe name must match remote pipe name"));
        }
        
        if (!localPipeAddress.isEphemeral()) {
        	return newFailedFuture(new NamedPipeException("Local NamedPipeAddress must be ephemeral"));
        }
        
        NamedPipeSession localSession = new NamedPipeSession(this, processor, localPipeAddress, getHandler());
        initSession(localSession, future, sessionInitializer);
        
        try {
        	IoFilterChain filterChain = localSession.getFilterChain();
            getFilterChainBuilder().buildFilterChain(filterChain);

            NamedPipeSession remoteSession = acceptor.newSession(remotePipeAddress, localSession);
            localSession.setRemoteSession(remoteSession);
            
            getListeners().fireSessionCreated(localSession);

        }
        catch (Throwable t) {
            future.setException(t);
            return future;
        }

        return future;
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		// no worker thread
		return null;
	}
	

}
