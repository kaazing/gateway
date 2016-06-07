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
package org.kaazing.gateway.service;

/**
 * Interface describing a Kaazing WebSocket Gateway service.
 */
public interface Service {
    /**
     * gets the internal type of the service.
     * @return the service type
     */
    String getType();
    
    /**
     * Initializes the service.
     * 
     * @throws Exception
     */
    void init(ServiceContext serviceContext) throws Exception;

    /**
     * Starts the service.
     * 
     * @throws Exception
     */
    void start() throws Exception;

    /**
     * Stops the service.
     * 
     * @throws Exception
     */
    void stop() throws Exception;

    /**
     * Quiesces the service.
     * 
     * @throws Exception
     */
    void quiesce() throws Exception;

    /**
     * Destroys the service.
     * 
     * @throws Exception
     */
    void destroy() throws Exception;

}
