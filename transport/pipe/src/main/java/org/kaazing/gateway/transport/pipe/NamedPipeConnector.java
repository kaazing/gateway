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

import javax.annotation.Resource;

import org.apache.mina.core.service.IoConnector;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.gateway.transport.SocketAddressFactory;
import org.kaazing.gateway.transport.bio.AbstractBioConnector;
import org.slf4j.LoggerFactory;

public class NamedPipeConnector extends AbstractBioConnector<NamedPipeAddress> {

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;
    private NamedPipeAcceptor acceptor;

    public NamedPipeConnector() {
        super(LoggerFactory.getLogger("transport.pipe"));
    }

	@Resource(name = "pipe.acceptor")
	public void setNamedPipeAcceptor(NamedPipeAcceptor acceptor) {
		this.acceptor = acceptor;
	}

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

	@Override
	protected SocketAddressFactory<NamedPipeAddress> initSocketAddressFactory() {
		return new NamedPipeAddressFactory();
	}

    @Override
    protected ResourceAddressFactory initResourceAddressFactory() {
        return resourceAddressFactory;
    }

    @Override
    protected BridgeServiceFactory initBridgeServiceFactory() {
        return bridgeServiceFactory;
    }

    @Override
	protected IoConnector initConnector() {
		NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
		connector.setNamedPipeAcceptor(new Ref<NamedPipeAcceptorImpl>() {
			@Override
            public NamedPipeAcceptorImpl get() {
				return (acceptor != null) ? acceptor.getAcceptor() : null;
			}
		});
		return connector;
	}

    @Override
    protected String getTransportName() {
        return "pipe";
    }

}
