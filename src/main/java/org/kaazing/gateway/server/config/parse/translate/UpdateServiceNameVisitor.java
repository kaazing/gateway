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

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

public class UpdateServiceNameVisitor extends AbstractVisitor {

    private static final String SERVICE_NODE = "service";
    private Namespace namespace;

    public UpdateServiceNameVisitor() {
        super();
    }

    @Override
    public void visit(Element element) throws Exception {
        Element typeElement = element.getChild("type", namespace);
        String type = typeElement.getTextTrim();
        if (type.equalsIgnoreCase("stomp.jms")) {
            typeElement.setText("jms");
        } else if (type.equals("stomp.interceptor")) {
            typeElement.setText("jms.proxy");
        } else if (type.equals("stomp.proxy")) {
            throw new RuntimeException("stomp.proxy is no longer supported, please migrate to jms.proxy instead");
        }
    }

    @Override
    public void translate(Document dom) throws Exception {
        Element root = dom.getRootElement();
        namespace = root.getNamespace();
        List<Element> children = dom.getRootElement().getChildren(SERVICE_NODE, namespace);
        for (Element child : children) {
            visit(child);
        }
    }
}
