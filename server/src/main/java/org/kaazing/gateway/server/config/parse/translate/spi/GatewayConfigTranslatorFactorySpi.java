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
package org.kaazing.gateway.server.config.parse.translate.spi;

import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.translate.GatewayConfigTranslator;

/**
 * Classes which translate/transform a DOM representing the config file implement this interface.  These classes are used by the
 * {@link GatewayConfigParser}
 */
public interface GatewayConfigTranslatorFactorySpi {

    /**
     * Given an incoming namespace, return the translator pipeline
     * to translate a document with that namespace up to the 'current' format.
     *
     * @param namespace  A namespace like "http://xmlns.kaazing.org/2014/09/gateway"
     * @return A translator to handle the namespace, or
     */
    GatewayConfigTranslator getTranslator(GatewayConfigNamespace ns);

}
