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
package org.kaazing.gateway.server.observer;

import java.util.Properties;

import javax.annotation.Resource;

import org.kaazing.gateway.server.GatewayObserverFactorySpi;
import org.kaazing.gateway.service.ServiceContext;

/**
 * 
 * Methods of this class will be called by every Gateway.start in the tests packages.
 *
 */
public class CounterGatewayObserver implements GatewayObserverFactorySpi {

    public static int COUNT_INSTANCES = 0;

    public static int COUNT_INITING_SERVICE = 0;

    public static Properties configuration;

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        CounterGatewayObserver.configuration = configuration;
    }

    public CounterGatewayObserver() {
        COUNT_INSTANCES++;
    }

    @Override
    public void initingService(ServiceContext serviceContext) {
        COUNT_INITING_SERVICE++;
    }
}
