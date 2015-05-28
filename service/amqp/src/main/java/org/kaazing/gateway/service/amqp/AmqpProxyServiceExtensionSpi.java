/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kaazing.gateway.service.amqp;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.ServiceProperties;

public abstract class AmqpProxyServiceExtensionSpi {
    /**
     * Whenever a new client connection is received (on the configured "accept")
     * the extension is giving an opportunity to initialize the associated session.
     *
     * @param acceptSession - the accept side of the end-to-end proxy connection
     * @param properties - the service properties that might contain useful values, for
     *                     example the VIRTUAL_HOST to use
     */
    public abstract void initAcceptSession(IoSession acceptSession, ServiceProperties properties);
}
