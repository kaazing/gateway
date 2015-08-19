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

package org.kaazing.gateway.server.config.parse.translate.aug2012;

import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class RemoveClusterModeVisitor extends AbstractVisitor {
    private static final String CLUSTER_ELEMENT = "cluster";
    private static final String MODE_ELEMENT = "mode";

    @Override
    public void visit(Element element) throws Exception {
        // Step 1 - get the mode element from the cluster element (same namespace as the cluster element)
        Element modeElement = element.getChild(MODE_ELEMENT, element.getNamespace());

        // Step 2 - remove the mode element from the cluster element
        element.removeContent(modeElement);

        // Step 3 - profit!
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        // There is at most one cluster element
        ElementFilter nameFilter = new ElementFilter(CLUSTER_ELEMENT);
        Iterator<?> iter = root.getDescendants(nameFilter);
        if (iter.hasNext()) {
            Element clusterElement = (Element) iter.next();
            iter = null; // so we don't get concurrent mod exception
            visit(clusterElement);
        }
    }
}
