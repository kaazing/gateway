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
package org.kaazing.gateway.transport.http.resource;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableMap;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class HttpDynamicResourceFactory {

    public static HttpDynamicResourceFactory newHttpDynamicResourceFactory() {
        return newHttpDynamicResourceFactory(currentThread().getContextClassLoader());
    }

    public static HttpDynamicResourceFactory newHttpDynamicResourceFactory(ClassLoader classLoader) {
        ServiceLoader<HttpDynamicResourceFactorySpi> loader = loadHttpDynamicResourceFactorySpi(classLoader);
        
        // load HttpDynamicResourceFactorySpi instances
        ConcurrentMap<String, HttpDynamicResourceFactorySpi> dynamicResourceFactories = new ConcurrentHashMap<>();
        for (HttpDynamicResourceFactorySpi dynamicResourceFactorySpi : loader) {
            Collection<String> resourceNames = dynamicResourceFactorySpi.getResourceNames();
            for (String resourceName : resourceNames) {
                HttpDynamicResourceFactorySpi oldDynamicResourceFactorySpi = dynamicResourceFactories.putIfAbsent(resourceName, dynamicResourceFactorySpi);
                if (oldDynamicResourceFactorySpi != null) {
                    // TODO: ignore, log warning?
                    throw new RuntimeException(format("Duplicate resouce name dynamic resource factory: %s", resourceName));
                }
            }
        }

        return new HttpDynamicResourceFactoryImpl(dynamicResourceFactories);
    }

    public abstract Collection<String> getResourceNames();
    
    public abstract HttpDynamicResource newHttpDynamicResource(String resourceName);

    private HttpDynamicResourceFactory() {
        // utility only, no instances
    }

    private static ServiceLoader<HttpDynamicResourceFactorySpi> loadHttpDynamicResourceFactorySpi(ClassLoader classLoader) {
        Class<HttpDynamicResourceFactorySpi> service = HttpDynamicResourceFactorySpi.class;
        return (classLoader != null) ? ServiceLoader.load(service, classLoader) : ServiceLoader.load(service);
    }

    private static final class HttpDynamicResourceFactoryImpl extends HttpDynamicResourceFactory {
        
        private final Map<String, HttpDynamicResourceFactorySpi> dynamicResourceFactories;
        
        @Override
        public Collection<String> getResourceNames() {
            return dynamicResourceFactories.keySet();
        }

        @Override
        public HttpDynamicResource newHttpDynamicResource(String resourceName) {
            HttpDynamicResourceFactorySpi dynamicResourceFactory = findDynamicResourceFactorySpi(resourceName);
            return dynamicResourceFactory.newDynamicResource(resourceName);
        }
        
        private HttpDynamicResourceFactoryImpl(Map<String, HttpDynamicResourceFactorySpi> dynamicResourceFactories) {
            this.dynamicResourceFactories = unmodifiableMap(dynamicResourceFactories);
        }

        private HttpDynamicResourceFactorySpi findDynamicResourceFactorySpi(String resourceName) {
            if (resourceName == null) {
                throw new NullPointerException("resourceName");
            }

            HttpDynamicResourceFactorySpi dynanmicResourceFactory = dynamicResourceFactories.get(resourceName);
            if (dynanmicResourceFactory == null) {
                throw new IllegalArgumentException(format("Unable to load resource '%s': No appropriate dynamic resource factory found", resourceName));
            }
            
            return dynanmicResourceFactory;
        }
    }
}
