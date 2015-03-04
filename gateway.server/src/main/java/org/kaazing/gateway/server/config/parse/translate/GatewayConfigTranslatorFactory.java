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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classes which translate/transform a DOM representing the config file implement this interface.  These classes are used by the
 * {@link GatewayConfigParser}
 */
public class GatewayConfigTranslatorFactory {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfigTranslatorFactory.class);

    private static final GatewayConfigTranslatorFactory instance = new GatewayConfigTranslatorFactory();

    protected GatewayConfigTranslatorFactory() {
    }

    public static GatewayConfigTranslatorFactory getInstance() {
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
        GatewayConfigTranslatorPipeline pipeline = new GatewayConfigTranslatorPipeline();

        switch (ns) {

            case SEPTEMBER_2014:
                // Currently no per-namespace translator to add in here, just validate
                GatewayConfigTranslator september2014Validator = new September2014Validator();
                pipeline.addTranslator(september2014Validator);
        }

        return pipeline;
    }
}
