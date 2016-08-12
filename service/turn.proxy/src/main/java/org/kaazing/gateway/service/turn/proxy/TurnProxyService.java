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
package org.kaazing.gateway.service.turn.proxy;

import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyService;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;

import javax.annotation.Resource;
import java.util.Properties;

public class TurnProxyService extends AbstractProxyService<TurnProxyAcceptHandler> {

    public static final String SERVICE_TYPE = "turn.proxy";
    private final TurnProxyAcceptHandler turnProxyHandler = new TurnProxyAcceptHandler();
    private SecurityContext securityContext;
    private Properties configuration;

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "securityContext")
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public String getType() {
        return SERVICE_TYPE;
    }

    @Override
    protected TurnProxyAcceptHandler createHandler() {
        return turnProxyHandler;
    }

    @Override
    public void init(ServiceContext serviceCtx) throws Exception {
        EarlyAccessFeatures.TURN_PROXY.assertEnabled(configuration, serviceCtx.getLogger());
        turnProxyHandler.init(serviceCtx, securityContext);
        super.init(serviceCtx);
    }
}
