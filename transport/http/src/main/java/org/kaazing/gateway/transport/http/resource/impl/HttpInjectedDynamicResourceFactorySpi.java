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
package org.kaazing.gateway.transport.http.resource.impl;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableMap;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResourceFactorySpi;

public final class HttpInjectedDynamicResourceFactorySpi extends HttpDynamicResourceFactorySpi {

    private static final String CROSS_ORIGIN_POSTMESSAGE_CHILD = "xsc";
    private static final String CROSS_ORIGIN_POSTMESSAGE_PARENT = "xsp";

    private final Map<String, HttpDynamicResource> resourcesByName;
    
    public HttpInjectedDynamicResourceFactorySpi() {
        Map<String, HttpDynamicResource> resourcesByName = new HashMap<>();
        ClassLoader classLoader = currentThread().getContextClassLoader();
        try {
            String resourcesPath = "META-INF/services/org/kaazing/gateway/server/resources";
            Enumeration<URL> resources = classLoader.getResources(resourcesPath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = new Properties();
                InputStream stream = resource.openStream();
                try {
                    properties.load(stream);
                    for (String resourceName : properties.stringPropertyNames()) {
                        String resourcePath = properties.getProperty(resourceName);
                        
                        // attempt to eliminate postMessage tampering detected message
                        if (CROSS_ORIGIN_POSTMESSAGE_PARENT.equals(resourceName) || 
                                CROSS_ORIGIN_POSTMESSAGE_CHILD.equals(resourceName)) {
                            Map<String, String> writeHeaders = Collections.singletonMap("Cache-control", "private, must-revalidate");
                            resourcesByName.put(resourceName, new HttpInjectedDynamicResource(resourcePath, writeHeaders));
                        }
                        else {
                            resourcesByName.put(resourceName, new HttpInjectedDynamicResource(resourcePath));
                        }
                    }
                }
                finally {
                    stream.close();
                }
            }
        } catch (Exception e) {
            // TODO: log warning
        }
        this.resourcesByName = unmodifiableMap(resourcesByName);
    }

    @Override
    public Collection<String> getResourceNames() {
        return resourcesByName.keySet();
    }

    @Override
    public HttpDynamicResource newDynamicResource(String resourceName) {
        return resourcesByName.get(resourceName);
    }

}
