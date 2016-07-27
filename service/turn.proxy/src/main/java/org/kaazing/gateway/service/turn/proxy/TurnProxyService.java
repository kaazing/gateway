package org.kaazing.gateway.service.turn.proxy;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyService;

public class TurnProxyService extends AbstractProxyService<TurnProxyHandler>{

    public static final String SERVICE_TYPE = "turn.proxy";
    private final TurnProxyHandler turnProxyHandler = new TurnProxyHandler();

    @Override
    public String getType() {
        return SERVICE_TYPE;
    }

    @Override
    protected TurnProxyHandler createHandler() {
        return turnProxyHandler;
    }
    
    @Override
    public void init(ServiceContext serviceCtx) throws Exception{
        turnProxyHandler.init(serviceCtx);
        super.init(serviceCtx);
    }

}
