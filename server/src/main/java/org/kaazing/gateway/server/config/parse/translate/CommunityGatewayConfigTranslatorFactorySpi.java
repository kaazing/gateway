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

import static org.kaazing.gateway.server.config.parse.GatewayConfigNamespace.NOVEMBER_2015;
import static org.kaazing.gateway.server.config.parse.GatewayConfigNamespace.SEPTEMBER_2014;

import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.translate.spi.GatewayConfigTranslatorFactorySpi;

/**
 * Classes which translate/transform a DOM representing the config file implement this interface.  These classes are used by the
 * {@link GatewayConfigParser}
 */
public class CommunityGatewayConfigTranslatorFactorySpi implements GatewayConfigTranslatorFactorySpi {

    private static final CommunityGatewayConfigTranslatorFactorySpi instance = new CommunityGatewayConfigTranslatorFactorySpi();

    public CommunityGatewayConfigTranslatorFactorySpi() {

    }

    public static CommunityGatewayConfigTranslatorFactorySpi getInstance() {
        return instance;
    }

    /**
     * Given an incoming namespace, return the translator pipeline
     * to translate a document with that namespace up to the 'current' format.
     *
     * @param ns
     * @return
     */
    @Override
    public GatewayConfigTranslator getTranslator(GatewayConfigNamespace ns) {
        // First, we create our pipeline composite
        GatewayConfigTranslatorPipeline result = null;

        if (ns.equals(GatewayConfigNamespace.SEPTEMBER_2014)) {
            result = new GatewayConfigTranslatorPipeline();
            GatewayConfigTranslator september2014Translator = new September2014ToNovember2015Translator();
            result.addTranslator(september2014Translator);
            ns = GatewayConfigNamespace.NOVEMBER_2015;
        }

        if (ns.equals(GatewayConfigNamespace.NOVEMBER_2015)) {
            if (result == null) {
                result = new GatewayConfigTranslatorPipeline();
            }
            GatewayConfigTranslator november2015Validator = new November2015ToJune2016Translator();
            result.addTranslator(november2015Validator);
            ns = GatewayConfigNamespace.CURRENT_NS;
        }

        if (ns.equals(GatewayConfigNamespace.CURRENT_NS)) {
            if (result == null) {
                result = new GatewayConfigTranslatorPipeline();
            }
            GatewayConfigTranslator june2016Validator = new June2016Validator();
            result.addTranslator(june2016Validator);
        }
        return result;
    }
}
