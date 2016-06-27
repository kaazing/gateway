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

package org.kaazing.gateway.server.config.parse.translate;

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
    public GatewayConfigTranslator getTranslator(GatewayConfigNamespace ns) {
        // First, we create our pipeline composite
        GatewayConfigTranslatorPipeline result = null;

        if (ns.equals(GatewayConfigNamespace.CURRENT_NS)) {
                // Currently no per-namespace translator to add in here, just validate
                result = new GatewayConfigTranslatorPipeline();
                GatewayConfigTranslator september2014Validator = new September2014Validator();
                result.addTranslator(september2014Validator);
        }

        return result;
    }
}
