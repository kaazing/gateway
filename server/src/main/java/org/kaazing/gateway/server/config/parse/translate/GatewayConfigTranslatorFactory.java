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
                // First validate that the config adhere's to the configuration guidelines
                // for the September 2014 schema
                GatewayConfigTranslator september2014Validator = new September2014Validator();
                pipeline.addTranslator(september2014Validator);

                // NOTE: We deliberately do NOT add a 'break' here; we want
                // the code to fall through to the next case, so that any
                // per-namespace pipelines also get added.  This is how we
                // "chain" the pipelines together, creating a multi-stage
                // pipeline to get a config from one namespace to the next.
                //
                // This means that the switch statement MUST have the
                // namespaces labels in chronological order, oldest to newest.

            case MARCH_2015:
                // The March 2015 schema enforces an order of elements in the accept-options
                // and connect-options.  Make sure that this translator is always run so that
                // config files built with this schema or any earlier schema do not require
                // a specific order, but instead the elements are correctly ordered at runtime
                // before the parsing/validation.
                GatewayConfigTranslator march2015Translator = new March2015Translator();
                pipeline.addTranslator(march2015Translator);

        }

        return pipeline;
    }
}
