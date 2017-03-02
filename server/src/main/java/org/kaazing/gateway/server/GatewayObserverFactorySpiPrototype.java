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
package org.kaazing.gateway.server;

import java.util.Map;
import java.util.Properties;

import org.kaazing.gateway.server.config.june2016.LoginModuleOptionsType;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.service.ServiceContext;

/**
 * This is a convenience class that can be extended to minimalize method Overrides in implementations of the the
 * GatewayObserverAPI
 *
 */
public abstract class GatewayObserverFactorySpiPrototype extends GatewayObserverFactorySpi {

    @Override
    public void initingService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void initedService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void startingService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void startedService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void stopingService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void stoppedService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void quiesceingService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void quiescedService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void destroyingService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void destroyedService(ServiceContext serviceContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void startingGateway(GatewayContext gatewayContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void stoppedGateway(GatewayContext gatewayContext) {
        // NOOP: to be extended as desired
    }

    @Override
    public void initingGateway(Properties properties, Map<String, Object> injectables) {
        // NOOP: to be extended as desired
    }

    @Override
    public void parseCustomOptions(Map<String, Object> options, LoginModuleOptionsType rawOptions) {
        // NOOP: to be extended as desired
    }

}
