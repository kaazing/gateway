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

import org.apache.log4j.Logger;

import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.translate.aug2012.JmxManagementVisitor;
import org.kaazing.gateway.server.config.parse.translate.aug2012.McpAcceptConnectToUdpAcceptConnectTranslator;
import org.kaazing.gateway.server.config.parse.translate.aug2012.SnmpManagementVisitor;
import org.kaazing.gateway.server.config.parse.translate.aug2012.NetworkElementIsNotSupportedVisitor;
import org.kaazing.gateway.server.config.parse.translate.aug2012.RemoveClusterModeVisitor;
import org.kaazing.gateway.server.config.parse.translate.excalibur.ReorderingVisitor;

/**
 * Classes which translate/transform an August2012 (3.4) config file DOM into a September2012 (restructured management)
 * config file DOM. These classes are used by the {@link GatewayConfigParser}
 */
public class August2012ToSeptember2012Translator extends GatewayConfigTranslatorPipeline {

    private static final Logger logger = Logger.getLogger(August2012ToSeptember2012Translator.class);

    public August2012ToSeptember2012Translator() {
        super();

        // These translators implement the functionality desired for
        // transforming a <management> element into the new Console-related
        // services (see KG-7454).
        addTranslator(new JmxManagementVisitor());
        addTranslator(new SnmpManagementVisitor());
        addTranslator(new RemoveElementVisitor("management"));

        // Add a unique name to each service
        addTranslator(new AddServiceNameVisitor());

        // Update old service names to new ones
        addTranslator(new UpdateServiceNameVisitor());

        // Remove mode from the cluster configuration
        addTranslator(new RemoveClusterModeVisitor());

        // If we see a network element we bork
        addTranslator(new NetworkElementIsNotSupportedVisitor());

        addTranslator(new McpAcceptConnectToUdpAcceptConnectTranslator());

        // It's important that this visitor, which reorders the added elements
        // such that the sequenced XSD requirements are met, is processed
        // after any translators which add/remove elements have been run.
        addTranslator(new ReorderingVisitor());

        // Set the September 2012 namespace. Add this to the end of the pipeline,
        // so that any nodes that are added will have the correct namespace set.
        addTranslator(new NamespaceVisitor(GatewayConfigNamespace.SEPTEMBER_2012));
    }
}
