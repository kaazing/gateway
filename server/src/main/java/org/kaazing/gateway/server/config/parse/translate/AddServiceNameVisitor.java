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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

/**
 * Visitor to translate 3.3-and-before 'management' section to a management.jmx-type service for the 2012/09 gateway
 * config XSD.
 *
 */
public class AddServiceNameVisitor extends AbstractVisitor {

    private static final String SERVICE_NAME = "service";

    // HashMap to make service type to # elements of that type
    private List<String> serviceNames = new ArrayList<String>();
    private int counter;

    private static final Logger logger = Logger.getLogger(AddServiceNameVisitor.class);

    public AddServiceNameVisitor() {
        super();
    }

    @Override
    public void visit(Element current) {
        // Get the name element (0 or 1)
        Element serviceNameElt = findSubElement(current, "name");
        String serviceName = serviceNameElt != null ? serviceNameElt.getTextTrim() : null;

        if (serviceName != null && serviceName.length() > 0) {
            return;
        }

        Element serviceTypeElt = findSubElement(current, "type");
        String serviceType = serviceTypeElt != null ? serviceTypeElt.getTextTrim() : null;

        // try the easy one first
        String baseName = serviceType == null || serviceType.trim().length() == 0 ? "SERVICE" : serviceType;

        counter = 2;
        serviceName = baseName;
        while (serviceNames.indexOf(serviceName) >= 0) {
            serviceName = baseName + counter;
            counter++;
        }

        serviceNames.add(serviceName);

        if (serviceNameElt == null) {
            serviceNameElt = new Element("name", current.getNamespace());
            serviceNameElt.addContent(serviceName);
            current.addContent(0, serviceNameElt);
        } else {
            serviceNameElt.setText(serviceName);
        }
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        ElementFilter serviceFilter = new ElementFilter(SERVICE_NAME);
        Iterator iter = root.getDescendants(serviceFilter);
        Element serviceNameElt;
        Element serviceTypeElt;

        // Since we may need to modify the services to add a name, we have
        // to extract the service Elements from the iterator before going
        // through them, since the iterator is actually an iterator over
        // the DOCUMENT TREE, not just the service elements, so modifying
        // them to add the name element results in a
        // ConcurrentModifcationException of the entire tree iterator.
        List<Element> serviceElements = new ArrayList<Element>();

        // Make first pass through the services to collect all the existing
        // service names. The following is not particularly elegant!
        while (iter.hasNext()) {
            Element serviceElement = (Element) iter.next();
            serviceElements.add(serviceElement);

            serviceNameElt = findSubElement(serviceElement, "name");
            serviceTypeElt = findSubElement(serviceElement, "type");
            String serviceName = serviceNameElt != null ? serviceNameElt.getTextTrim() : null;
            String serviceType = serviceTypeElt != null ? serviceTypeElt.getTextTrim() : null;

            if (serviceName != null && serviceName.length() > 0) {
                serviceNames.add(serviceName);
            }
        }

        iter = null;

        for (Element serviceElement : serviceElements) {
            visit(serviceElement);
        }
    }

    public Element findSubElement(Element current, String subElementName) {
        ElementFilter filter = new ElementFilter(subElementName);
        Iterator iter = current.getDescendants(filter);
        Element val = null;
        if (iter.hasNext()) {
            val = (Element) iter.next();
        }

        iter = null;
        return val;
    }
}
