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

import org.apache.mina.core.service.IoAcceptor;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.gateway.transport.SocketAddressFactory;
import org.kaazing.gateway.transport.bio.AbstractBioAcceptor;

public class NamedPipeAcceptor extends AbstractBioAcceptor<NamedPipeAddress> {

//	private final Logger logger = LoggerFactory.getLogger("transport.pipe");
    private ResourceAddressFactory resourceAddressFactory;
    private BridgeServiceFactory bridgeServiceFactory;

    @Override
    protected String getTransportName() {
        return "pipe";
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
    protected ResourceAddressFactory initResourceAddressFactory() {
        return resourceAddressFactory;
    }

    @Override
    protected BridgeServiceFactory initBridgeServiceFactory() {
        return bridgeServiceFactory;
    }

	@Override
	protected IoAcceptor initAcceptor() {
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
		return acceptor;
	}

	@Override
	protected SocketAddressFactory<NamedPipeAddress> initSocketAddressFactory() {
		return new NamedPipeAddressFactory();
	}


    @Override
	protected NamedPipeAcceptorImpl getAcceptor() {
		return (NamedPipeAcceptorImpl) super.getAcceptor();
	}

}
