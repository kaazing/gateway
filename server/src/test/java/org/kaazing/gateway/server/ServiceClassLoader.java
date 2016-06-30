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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;

public final class ServiceClassLoader extends ClassLoader {
    private final String servicePath;
    private final URL[] serviceURLs;

    public ServiceClassLoader(
            Class<?> serviceClass,
            URL... serviceURLs) {
        super();
        this.servicePath = format("META-INF/services/%s", serviceClass.getName());
        this.serviceURLs = serviceURLs;
    }

    public ServiceClassLoader(ClassLoader parent,
                              Class<?> serviceClass,
                              URL... serviceURLs) {
        super(parent);
        this.servicePath = format("META-INF/services/%s", serviceClass.getName());
        this.serviceURLs = serviceURLs;
    }

    @Override
    protected Enumeration<URL> findResources(String name)
            throws IOException {
        if (servicePath.equals(name)) {
            return enumeration(asList(serviceURLs));
        }
        return super.findResources(name);
    }
}
