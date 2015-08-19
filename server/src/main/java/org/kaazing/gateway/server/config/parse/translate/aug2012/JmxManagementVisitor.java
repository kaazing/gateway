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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;

/**
 * Visitor to translate 3.3-and-before 'management' section to a management.jmx-type service for the 2012/09 gateway
 * config XSD.
 *
 */
public class JmxManagementVisitor extends AbstractManagementVisitor {

    private static final String JMX_MANAGEMENT_TYPE = "management.jmx";

    private static final Logger logger = Logger.getLogger(JmxManagementVisitor.class);

    public JmxManagementVisitor() {
        super();
    }

    @Override
    public void visit(Element current) {

        // Get the accept element (exactly 1).
        String acceptURI = null;
        ElementFilter filter = new ElementFilter("accept");
        Iterator iter = current.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            Element elt = listIter.next();
            acceptURI = elt.getTextTrim();
        }

        // Get the property element
        Element propertiesElement = null;
        filter = new ElementFilter("properties");
        iter = current.getDescendants(filter);
        if (iter.hasNext()) {
            propertiesElement = (Element) iter.next();
        }

        // Find the realm-name element, if any (0 or 1 possible)
        Element realmNameElement = null;

        filter = new ElementFilter("realm-name");
        iter = current.getDescendants(filter);
        listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            realmNameElement = listIter.next();
        }

        // Find the authorization constraint elements, if any (0 or more)
        List<Element> authorizationConstraints = new ArrayList<Element>();

        filter = new ElementFilter("authorization-constraint");
        iter = current.getDescendants(filter);
        listIter = toListIterator(iter);

        while (listIter.hasNext()) {
            Element authConstraintElement = listIter.next();
            authorizationConstraints.add(authConstraintElement);
        }

        Element parent = current.getParentElement();
        Namespace ns = current.getNamespace();

        // Create a new <service> element for the JMX management service,
        // based on the old <management> data
        Element jmxServiceElement = new Element("service", ns);

        Element nameElement = new Element("name", ns);
        nameElement.setText("JMX Management");
        jmxServiceElement.addContent(nameElement);

        Element descriptionElement = new Element("description", ns);
        descriptionElement.setText("JMX Management Service");
        jmxServiceElement.addContent(descriptionElement);

        Element typeElement = new Element("type", ns);
        typeElement.setText(JMX_MANAGEMENT_TYPE);
        jmxServiceElement.addContent(typeElement);

        if (propertiesElement == null) {
            propertiesElement = new Element("properties", ns);
        }

        if (acceptURI != null && acceptURI.length() > 0) {
            Element addressElement = new Element("connector.server.address", ns);
            addressElement.setText(acceptURI);
            propertiesElement.addContent(addressElement);
        }

        // Cloned because we don't want to change the original node,
        // It is needed for SNMP Visitor
        Element propertiesClone = (Element) propertiesElement.clone();
        jmxServiceElement.addContent(propertiesClone.detach());

        if (realmNameElement != null) {
            Element jmxRealmElement = (Element) realmNameElement.clone();
            jmxRealmElement.setNamespace(ns);

            jmxServiceElement.addContent(jmxRealmElement);
        }

        if (authorizationConstraints.size() != 0) {
            List<Element> jmxAuthConstraints = new ArrayList<Element>(authorizationConstraints.size());
            for (Element authConstraint : authorizationConstraints) {
                Element jmxAuthConstraint = (Element) authConstraint.clone();
                jmxAuthConstraint.setNamespace(ns);

                jmxAuthConstraints.add(jmxAuthConstraint);
            }

            jmxServiceElement.addContent(jmxAuthConstraints);
        }

        // Now add the new <service> element into the DOM, in the proper place
        addService(jmxServiceElement, parent, "\n   The JMX Management Service\n  ");
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        // There is at most one management object.
        ElementFilter nameFilter = new ElementFilter(AbstractManagementVisitor.MANAGEMENT_NAME);
        Iterator iter = root.getDescendants(nameFilter);
        ListIterator<Element> listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            Element management = listIter.next();
            visit(management);
        }
    }
}
