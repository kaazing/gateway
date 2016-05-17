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

import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.ServiceLoader;

import javax.annotation.Resource;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeServiceFactory;

/**
 * Gateway service of type "proxy".
 */
public class ProxyService extends AbstractProxyService<ProxyServiceHandler> {
    private BridgeServiceFactory bridgeServiceFactory;

    public ProxyService() {
    }

    @Override
    public String getType() {
        return "proxy";
    }

    @Override
    protected ProxyServiceHandler createHandler() {
        return new ProxyServiceHandler();
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        super.init(serviceContext);
        Collection<String> connectURIs = serviceContext.getConnects();
        if (connectURIs == null || connectURIs.isEmpty()) {
            throw new IllegalArgumentException("Missing required element: <connect>");
        }
        ProxyServiceHandler handler = getHandler();
        handler.setConnectURIs(connectURIs);
        handler.initServiceConnectManager(bridgeServiceFactory);

        // Instantiate any proxy service extensions and register them with the handler
        ServiceLoader<ProxyServiceExtensionSpi> proxyServiceExtensions = load(ProxyServiceExtensionSpi.class);
        for (ProxyServiceExtensionSpi proxyServiceExtension : proxyServiceExtensions) {
            handler.registerExtension(proxyServiceExtension);
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
        getHandler().startServiceConnectManager();
    }

    // FIXME:  How should this be exposed to Management?  For now the service connect manager object is exposed through this method, but
    //         perhaps management could attach a listener that in turn gets passed to the handler and on to the connect manager...
    public ServiceConnectManager getServiceConnectManager() {
        return getHandler().getServiceConnectManager();
    }
}
