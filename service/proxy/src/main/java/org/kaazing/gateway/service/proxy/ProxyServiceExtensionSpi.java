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
package org.kaazing.gateway.service.proxy;

import org.kaazing.mina.core.session.IoSessionEx;

public abstract class ProxyServiceExtensionSpi {
    /**
     * The proxy service establishes a connection based on the configured "connect"
     * whenever a new client connection is received (on the configured "accept").
     * When this connect completes extensions are notified via
     * proxiedConnectionEstablished() and given both the IoSessionEx representing
     * the accept side of the end-to-end connection and the IoSessionEx representing
     * the connect side of the end-to-end connection.
     *
     * @param acceptSession - the accept side of the end-to-end proxy connection
     * @param connectSession - the connect side of the end-to-end proxy connection
     */
    public abstract void proxiedConnectionEstablished(IoSessionEx acceptSession, IoSessionEx connectSession);
}
