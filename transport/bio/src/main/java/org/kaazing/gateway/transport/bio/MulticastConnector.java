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

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.mina.core.service.IoConnector;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.SocketAddressFactory;
import org.slf4j.LoggerFactory;


public class MulticastConnector extends AbstractBioConnector<MulticastAddress> {

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    public MulticastConnector() {
        super(LoggerFactory.getLogger("transport.bio"));
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
    private Properties configuration;

	@Override
	protected SocketAddressFactory<MulticastAddress> initSocketAddressFactory() {
		return new MulticastAddressFactory();
	}

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Override
	protected IoConnector initConnector() {
		MulticastConnectorImpl connector = new MulticastConnectorImpl(resourceAddressFactory);
        String property = configuration.getProperty("org.kaazing.gateway.transport.udp.READ_BUFFER_SIZE");
        if (property != null) {
        	int readBufferSize = Integer.parseInt(property);
        	connector.getSessionConfig().setReadBufferSize(readBufferSize);
			logger.debug("READ_BUFFER_SIZE setting for Multicast connector: {}", readBufferSize);
        }
		return connector;
	}

    @Override
    protected String getTransportName() {
        return "mcp";
    }

}
