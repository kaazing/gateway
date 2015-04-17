/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.ws.extension.internal;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.transport.ws.extension.WsExtension;
import org.kaazing.gateway.transport.ws.extension.spi.WebSocketExtensionFactorySpi;
import org.kaazing.gateway.transport.ws.extension.spi.WebSocketExtensionSpi;


public final class WebSocketExtensionFactory {
    private static final Pattern PATTERN_EXTENSION_FORMAT = Pattern.compile("([a-zA-Z0-9]*)(;?(.*))");

    private final Map<String, WebSocketExtensionFactorySpi> factoriesRO;

    private WebSocketExtensionFactory(Map<String, WebSocketExtensionFactorySpi> factoriesRO) {
        this.factoriesRO = factoriesRO;
    }

    /**
     * Creates and returns {@link WebSocketExtensionSpi} instance representing the extension using the registered
     * {@link WebSocketExtensionFactorySpi}. The format of the specified extensionWithParams is as shown below:
     *
     * {@code}
     *      extension-name[;param1=value1;param2;param3=value3]
     * {@code}
     *
     * @param extensionWithParams  String representation of the extension in request header format
     *
     * @return WebSocketExtensionSpi instance
     */
    public WebSocketExtensionSpi createExtension(String extensionWithParams) throws IOException {
        Matcher extensionMatcher = PATTERN_EXTENSION_FORMAT.matcher(extensionWithParams);
        if (!extensionMatcher.matches()) {
            throw new IllegalStateException(format("Bad extension syntax: %s", extensionWithParams));
        }

        String extensionName = extensionMatcher.group(1);

        WebSocketExtensionFactorySpi factory = factoriesRO.get(extensionName);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported extension: " + extensionName);
        }

        return factory.createExtension(extensionWithParams);
    }

    /**
     * Returns the names of all the supported/discovered extensions.
     *
     * @return Collection of extension names
     */
    public Collection<String> getExtensionNames() {
        return factoriesRO.keySet();
    }

    /**
     * Validates the extension name, parameter names and values in the specified string. The format of the specified
     * extensionWithParams is as shown below:
     *
     * {@code}
     *      extension-name[;param1=value1;param2;param3=value3]
     * {@code}
     * @param extensionWithParams  String representation of the extension in request header format
     * @return WebSocketExtensionSpi instance
     */
    public void validateExtension(WsExtension extensionWithParams) throws IOException {
        String extensionName = extensionWithParams.getExtensionToken();

        WebSocketExtensionFactorySpi factory = factoriesRO.get(extensionName);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported extension: " + extensionName);
        }

        factory.validate(extensionWithParams);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the default {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static WebSocketExtensionFactory newInstance() {
        ServiceLoader<WebSocketExtensionFactorySpi> services = load(WebSocketExtensionFactorySpi.class);
        return newInstance(services);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the specified {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static WebSocketExtensionFactory newInstance(ClassLoader cl) {
        ServiceLoader<WebSocketExtensionFactorySpi> services = load(WebSocketExtensionFactorySpi.class, cl);
        return newInstance(services);
    }


    private static WebSocketExtensionFactory newInstance(ServiceLoader<WebSocketExtensionFactorySpi> services) {
        Map<String, WebSocketExtensionFactorySpi> factories = new HashMap<String, WebSocketExtensionFactorySpi>();
        for (WebSocketExtensionFactorySpi service : services) {
            String extensionName = service.getExtensionName();
            factories.put(extensionName, service);
        }
        return new WebSocketExtensionFactory(unmodifiableMap(factories));
    }
}
