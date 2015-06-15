package org.kaazing.gateway.transport;

import javax.annotation.Resource;

import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.TransportFactory;

public class TestTransportExtension {

    private TransportFactory transportFactory;
    private ResourceAddressFactory resourceAddressFactory;

    public ResourceAddressFactory getResourceAddressFactory() {
        return resourceAddressFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    @Resource(name = "transportFactory")
    public void setTransportFactory(TransportFactory bridgeServiceFactory) {
        this.transportFactory = bridgeServiceFactory;
    }

}