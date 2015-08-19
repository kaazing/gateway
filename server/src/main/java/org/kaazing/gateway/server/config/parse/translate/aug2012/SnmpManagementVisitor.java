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

import java.net.URI;
import java.net.URISyntaxException;
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
 * Visitor to translate 3.3-and-before 'management' section to a management.snmp-type service
 *  for the 2012/09 gateway config XSD.
 *
 **/
public class SnmpManagementVisitor extends AbstractManagementVisitor {

    private static final String SNMP_MANAGEMENT_TYPE = "management.snmp";

    // Ideally we would use '${gateway.hostname}' here instead of localhost.
    // The problem is that we cannot guarantee that the admin has configured
    // a 'gateway.hostname' property, sadly.

    private static String SNMP_CLUSTER_ACCEPT_URI = "ws://localhost:8080/snmp";
    private static String SNMP_STANDALONE_ACCEPT_URI = "ws://localhost:8000/snmp";

    private static final String CLUSTER_NAME = "cluster";

    private static final Logger logger = Logger.getLogger(SnmpManagementVisitor.class);

    private Element clusterElement;

    public SnmpManagementVisitor() {
        super();
    }

    @Override
    public void visit(Element current) {

        // Find accept URI domain name
        String acceptURIHost = null;

        String acceptURI = null;

        ElementFilter filter = new ElementFilter("accept");
        Iterator iter = current.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            Element elt = listIter.next();
            acceptURI = elt.getTextTrim();
        }

        if (acceptURI != null) {
            try {
                if (acceptURI.contains("${gateway.hostname}")) {
                    SNMP_CLUSTER_ACCEPT_URI = "ws://${gateway.hostname}:8080/snmp";
                    SNMP_STANDALONE_ACCEPT_URI = "ws://${gateway.hostname}:8000/snmp";
                } else {
                    URI hostnameExtractor;
                    hostnameExtractor = new URI(acceptURI);
                    acceptURIHost = hostnameExtractor.getHost();
                    if (acceptURI != null) {
                        SNMP_CLUSTER_ACCEPT_URI = String.format("ws://%s:8080/snmp", acceptURIHost);
                        SNMP_STANDALONE_ACCEPT_URI = String.format("ws://%s:8000/snmp", acceptURIHost);
                    }
                }
            } catch (URISyntaxException e) {
                // Fallback to localhost
            }
        }

        // Find the realm-name element, if any (0 or 1 possible)
        Element realmNameElement = null;

        ElementFilter realmFilter = new ElementFilter("realm-name");
        Iterator realmIter = current.getDescendants(realmFilter);
        ListIterator<Element> realmListIter = toListIterator(realmIter);

        if (realmListIter.hasNext()) {
            realmNameElement = realmListIter.next();
        }

        // Find the authorization constraint elements, if any (0 or more)
        List<Element> authorizationConstraints = new ArrayList<Element>();

        realmFilter = new ElementFilter("authorization-constraint");
        realmIter = current.getDescendants(realmFilter);
        realmListIter = toListIterator(realmIter);

        while (realmListIter.hasNext()) {
            Element authConstraintElement = realmListIter.next();
            authorizationConstraints.add(authConstraintElement);
        }

        Element parent = current.getParentElement();
        Namespace ns = current.getNamespace();

        // Create a new <service> element for the SNMP management service,
        // based on the old <management> data
        Element snmpServiceElement = new Element("service", ns);

        Element nameElement = new Element("name", ns);
        nameElement.setText("SNMP Management");
        snmpServiceElement.addContent(nameElement);

        Element descriptionElement = new Element("description", ns);
        descriptionElement.setText("SNMP Management Service");
        snmpServiceElement.addContent(descriptionElement);

        Element acceptElement = new Element("accept", ns);
        if (clusterElement != null) {
            acceptElement.setText(SNMP_CLUSTER_ACCEPT_URI);

        } else {
            acceptElement.setText(SNMP_STANDALONE_ACCEPT_URI);
        }

        snmpServiceElement.addContent(acceptElement);

        Element typeElement = new Element("type", ns);
        typeElement.setText(SNMP_MANAGEMENT_TYPE);
        snmpServiceElement.addContent(typeElement);

        if (realmNameElement != null) {
            Element snmpRealmElement = (Element) realmNameElement.clone();
            snmpRealmElement.setNamespace(ns);

            snmpServiceElement.addContent(snmpRealmElement);
        }

        Element propertiesElement = null;
        filter = new ElementFilter("properties");
        iter = current.getDescendants(filter);
        if (iter.hasNext()) {
            propertiesElement = (Element) iter.next();
        }
        if (propertiesElement != null) {
            // Cloned because we don't want to change the original node,
            // It is needed for JMX Visitor
            Element propertiesClone = (Element) propertiesElement.clone();
            snmpServiceElement.addContent(propertiesClone.detach());
        }

        if (authorizationConstraints.size() != 0) {
            List<Element> snmpAuthConstraints = new ArrayList<Element>(authorizationConstraints.size());
            for (Element authConstraint : authorizationConstraints) {
                Element snmpAuthConstraint = (Element) authConstraint.clone();
                snmpAuthConstraint.setNamespace(ns);

                snmpAuthConstraints.add(snmpAuthConstraint);
            }

            snmpServiceElement.addContent(snmpAuthConstraints);
        }

        // Now add the new <service> element into the DOM, in the proper place
        addService(snmpServiceElement, parent, "\n   The SNMP Management Service\n  ");
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        // First, let's see if there is a <cluster> element
        ElementFilter filter = new ElementFilter(CLUSTER_NAME);
        Iterator iter = root.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            clusterElement = listIter.next();
        }

        // There is at most one <management> element
        filter = new ElementFilter(AbstractManagementVisitor.MANAGEMENT_NAME);
        iter = root.getDescendants(filter);
        listIter = toListIterator(iter);

        if (listIter.hasNext()) {
            Element management = listIter.next();
            visit(management);
        }
    }
}
