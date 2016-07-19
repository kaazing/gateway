package org.kaazing.gateway.service.turn.proxy;

import java.util.Collection;
import java.util.Collections;

import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceFactorySpi;

public class TurnProxyServiceFactorySpi extends ServiceFactorySpi {

    private static final Collection<String> SERVICE_TYPES = Collections.singletonList(TurnProxyService.SERVICE_TYPE);

    @Override
    public Collection<String> getServiceTypes() {
        return SERVICE_TYPES;
    }

    @Override
    public Service newService(String serviceType) {
        assert TurnProxyService.SERVICE_TYPE.equals(serviceType);
        return new TurnProxyService();
    }
}
