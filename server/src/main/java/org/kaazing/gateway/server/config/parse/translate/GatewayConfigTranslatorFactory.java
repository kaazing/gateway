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
package org.kaazing.gateway.server.config.parse.translate;

import static java.util.ServiceLoader.load;

import java.util.ServiceLoader;

import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.translate.spi.GatewayConfigTranslatorFactorySpi;

/**
 * Classes which translate/transform a DOM representing the config file implement this interface.  These classes are used by the
 * {@link GatewayConfigParser}
 */
public class GatewayConfigTranslatorFactory {

    private final ServiceLoader<GatewayConfigTranslatorFactorySpi> services;

    public GatewayConfigTranslatorFactory(ServiceLoader<GatewayConfigTranslatorFactorySpi> services) {
        this.services = services;
    }

    /**
     * Creates a new instance of GatewayConfigTranslatorFactory. It uses the default {@link ClassLoader} to load
     * {@link GatewayConfigTranslatorFactorySpi} objects that are registered using META-INF/services.
     *
     * @return GatewayConfigTranslatorFactory
     */
    public static GatewayConfigTranslatorFactory newInstance() {
        ServiceLoader<GatewayConfigTranslatorFactorySpi> services = load(GatewayConfigTranslatorFactorySpi.class);
        return newInstance(services);
    }

    /**
     * Creates a new instance of GatewayConfigTranslatorFactory. It uses the specified {@link ClassLoader} to load
     * {@link GatewayConfigTranslatorFactorySpi} objects that are registered using META-INF/services.
     *
     * @return GatewayConfigTranslatorFactory
     */
    public static GatewayConfigTranslatorFactory newInstance(ClassLoader cl) {
        ServiceLoader<GatewayConfigTranslatorFactorySpi> services = load(GatewayConfigTranslatorFactorySpi.class, cl);
        return newInstance(services);
    }

    private static GatewayConfigTranslatorFactory newInstance(ServiceLoader<GatewayConfigTranslatorFactorySpi> services) {
        return new GatewayConfigTranslatorFactory(services);
    }

    /**
     * Given an incoming namespace, return the translator pipeline
     * to translate a document with that namespace up to the 'current' format.
     *
     * @param ns
     * @return
     * @throws Exception
     */
    public GatewayConfigTranslator getTranslator(GatewayConfigNamespace namespace) throws Exception {
        GatewayConfigTranslator result;
        for (GatewayConfigTranslatorFactorySpi factory : services) {
            result = factory.getTranslator(namespace);
            if (result != null) {
                return result;
            }
        }
        throw new Exception("Unrecognized gateway configuration namespace " + namespace);
    }
}
