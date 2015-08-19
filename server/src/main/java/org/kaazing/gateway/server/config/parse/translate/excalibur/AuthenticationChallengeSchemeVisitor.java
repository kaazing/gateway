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

package org.kaazing.gateway.server.config.parse.translate.excalibur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

/**
 * Ensures that every <authentication> element, i.e.:
 *
 * <security> <realm> <authentication>
 *
 * has an <http-challenge-scheme> element. If one is not already present, one will be added, with a value of
 * "Application Basic".
 *
 * For a more information, see:
 *
 * https://docs.google.com/a/kaazing.com/document/d/1fTlnP7ZGUumIZVnNlkz9bYKzox79ZXIs3oocmvuIKko/edit
 *
 */
public class AuthenticationChallengeSchemeVisitor extends AbstractVisitor {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationChallengeSchemeVisitor.class);

    private static final String SECURITY_NAME = "security";
    private static final String REALM_NAME = "realm";
    private static final String AUTHN_NAME = "authentication";

    // <security/realm/authentication> elements
    private static final String HTTP_CHALLENGE_SCHEME_NAME = "http-challenge-scheme";

    public AuthenticationChallengeSchemeVisitor() {
        super();
    }

    private void visitSecurityRealmAuthentication(final RealmNode realmNode) {
        Element authn = realmNode.getAuthentication();
        Namespace ns = authn.getNamespace();

        // Make sure that there is an <http-challenge-scheme> element within the
        // <authentication> element.

        List kids = authn.getChildren();
        if (kids.size() > 0) {
            Iterator iter = kids.iterator();
            ListIterator<Element> listIter = toListIterator(iter);

            boolean haveChallengeScheme = false;

            while (listIter.hasNext()) {
                Element kid = listIter.next();

                if (kid.getName().equals(HTTP_CHALLENGE_SCHEME_NAME)) {
                    haveChallengeScheme = true;
                    break;
                }
            }

            if (!haveChallengeScheme) {
                Element httpChallengeScheme = new Element(HTTP_CHALLENGE_SCHEME_NAME, ns);
                httpChallengeScheme.setText("Application Basic");
                authn.addContent(httpChallengeScheme);
            }
        }
    }

    @Override
    public void visit(Element current) {
        // First, get all of the <security> elements...
        ElementFilter filter = new ElementFilter(SECURITY_NAME);
        Iterator securityIter = current.getDescendants(filter);

        // ...and visit each one
        while (securityIter.hasNext()) {
            Element security = (Element) securityIter.next();

            SecurityNode securityNode = new SecurityNode(security);

            List<RealmNode> securityRealmNodes = securityNode.getRealms();
            if (securityRealmNodes != null) {
                for (RealmNode realmNode : securityRealmNodes) {
                    visitSecurityRealmAuthentication(realmNode);
                }
            }
        }
    }

    // Helper classes

    // <security />
    private static class SecurityNode {
        private Element securityElement;
        private List<RealmNode> realmNodes;

        public SecurityNode(Element securityElement) {
            this.securityElement = securityElement;

            ElementFilter filter = new ElementFilter(REALM_NAME);
            Iterator iter = securityElement.getDescendants(filter);

            if (iter.hasNext()) {
                realmNodes = new ArrayList<RealmNode>(1);

                while (iter.hasNext()) {
                    Element realm = (Element) iter.next();
                    RealmNode realmNode = new RealmNode(realm);
                    realmNodes.add(realmNode);
                }
            }
        }

        public List<RealmNode> getRealms() {
            return realmNodes;
        }
    }

    // <security.realm />
    private static class RealmNode {
        private Element authnElement;
        private Element realmElement;
        private String realmName;

        public RealmNode(Element realmElement) {
            this.realmElement = realmElement;

            List kids = realmElement.getChildren();
            if (kids.size() > 0) {
                Iterator iter = kids.iterator();

                while (iter.hasNext()) {
                    Element kid = (Element) iter.next();

                    if (kid.getName().equals("name")) {
                        this.realmName = kid.getText();
                        continue;
                    }

                    if (this.authnElement == null) {
                        if (kid.getName().equals(AUTHN_NAME)) {
                            this.authnElement = kid;
                            continue;
                        }
                    }
                }
            }
        }

        public String getName() {
            return realmName;
        }

        public Element getElement() {
            return realmElement;
        }

        public Element getAuthentication() {
            return authnElement;
        }
    }
}
