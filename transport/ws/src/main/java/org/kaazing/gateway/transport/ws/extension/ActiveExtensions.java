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

package org.kaazing.gateway.transport.ws.extension;

import java.util.Collections;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.TypedAttributeKey;

/**
 * {@link ActiveExtensions} maintains a list of WebSocketExtensions that have successfully been negotiated. 
 * The list that it is constructed with has already properly ordered the extensions as required by the 
 * {@link WebSocketExtension}
 */
public class ActiveExtensions{
    
    public static final ActiveExtensions EMPTY = new ActiveExtensions(Collections.<WebSocketExtension>emptyList());
    private static final TypedAttributeKey<ActiveExtensions> WS_EXTENSIONS_KEY = new TypedAttributeKey<>(
            ActiveExtensions.class, "activeWsExtensions");

    private final List<WebSocketExtension> extensions = Collections.emptyList()

    public ActiveExtensions(List<WebSocketExtension> negotiatedExtensions) {
        extensions = Collections.unmodifiableList(negotiatedExtensions);
    }

    public List<WebSocketExtension> asList() {
        return extensions;
    }

    public static ActiveExtensions get(IoSession session) {
        ActiveExtensions extensions = WS_EXTENSIONS_KEY.get(session);
        return extensions == null ? EMPTY : extensions;
    }

    @SuppressWarnings("unchecked")
    public <T extends WebSocketExtension> T getExtension(Class<T> extensionClass) {
        for (WebSocketExtension extension : extensions) {
            if (extensionClass == extension.getClass()) {
                return (T) extension;
            }
        }
        return null;
    }

    public boolean hasExtension(Class<? extends WebSocketExtension> extensionClass) {
        return getExtension(extensionClass) != null;
    }

}
