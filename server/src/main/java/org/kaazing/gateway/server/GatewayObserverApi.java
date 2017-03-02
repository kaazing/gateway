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
 * GatewayObserverApi that allows classes to observe major gateway events. Implementing classes can be injected, @see
 * {@link GatewayObserverFactorySpi}.
 *
 */
public interface GatewayObserverApi {

    /**
     * Notification for when a service is being initiated
     * @param serviceContext
     */
    void initingService(ServiceContext serviceContext);

    /**
     * Notification that a service has been initiated
     * @param serviceContext
     */
    void initedService(ServiceContext serviceContext);

    /**
     * Notification that a service is starting
     * @param serviceContext
     */
    void startingService(ServiceContext serviceContext);

    /**
     * Notification that a service started
     * @param serviceContext
     */
    void startedService(ServiceContext serviceContext);

    /**
     * Notification that a service is being stopped
     * @param serviceContext
     */
    void stopingService(ServiceContext serviceContext);

    /**
     * Notification that a service has been stopped
     * @param serviceContext
     */
    void stoppedService(ServiceContext serviceContext);

    /**
     * Notification that a service is being quiesced
     * @param serviceContext
     */
    void quiesceingService(ServiceContext serviceContext);

    /**
     * Notification that a service has been quiesced
     * @param serviceContext
     */
    void quiescedService(ServiceContext serviceContext);

    /**
     * Notification that a service is being destroyed
     * @param serviceContext
     */
    void destroyingService(ServiceContext serviceContext);

    /**
     * Notification that a service has been destroyed
     * @param serviceContext
     */
    void destroyedService(ServiceContext serviceContext);

    /**
     * Notification that the gateway is starting
     * @param gatewayContext
     */
    void startingGateway(GatewayContext gatewayContext);

    /**
     * Notification that the gateway is stopped
     * @param gatewayContext
     */
    void stoppedGateway(GatewayContext gatewayContext);

    /**
     * Notification that the gateway is being initialized.
     * This provides a hook point to add injectable resources.
     * Note, injection will not be done at this point on any resource that the GatewayObserver depends on.
     * @param configuration
     * @param injectables
     */
    void initingGateway(Properties configuration, Map<String, Object> injectables);

        /**
    * Notification that the gateway is configured additional parsing of the file can be done and 
    * various custom properties can be set-up
    * @param options
    * @param rawOptions - the actual XML
    */
    void parseCustomOptions(Map<String, Object> options, LoginModuleOptionsType rawOptions);

}
