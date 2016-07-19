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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;

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
    void init(ServiceContext serviceContext);

    /**
     * Starts the service.
     * @throws URISyntaxException 
     * @throws MalformedURLException 
     * @throws RemoteException 
     * @throws IOException 
     * 
     * @throws Exception
     */
    void start() throws URISyntaxException, MalformedURLException, RemoteException, IOException;

    /**
     * Stops the service.
     * @throws IOException 
     * @throws URISyntaxException 
     * 
     * @throws Exception
     */
    void stop() throws IOException, URISyntaxException;

    /**
     * Quiesces the service.
     * @throws URISyntaxException 
     * 
     * @throws Exception
     */
    void quiesce() throws URISyntaxException;

    /**
     * Destroys the service.
     * 
     * @throws Exception
     */
    void destroy();

}
