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

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class ReorderingVisitor extends AbstractVisitor {

    // <service>
    private static final String SERVICE_NAME = "service";

    // <service/accept>
    private static final String SERVICE_NAME_NAME = "name";

    // <service/accept>
    private static final String SERVICE_DESCRIPTION_NAME = "description";

    // <service/accept>
    private static final String SERVICE_ACCEPT_NAME = "accept";

    // <service/type>
    private static final String SERVICE_TYPE_NAME = "type";

    // <service/properties>
    private static final String SERVICE_PROP_NAME = "properties";

    // <service/properties/service.domain>
    private static final String SERVICE_PROP_SERVICE_DOMAIN_NAME = "service.domain";

    // <service/properties/encryption.key.alias>
    private static final String SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME = "encryption.key.alias";

    // <service/properties/authentication.connect>
    private static final String SERVICE_PROP_AUTHN_CONNECT_NAME = "authentication.connect";

    // <service/properties/authentication.identifier>
    private static final String SERVICE_PROP_AUTHN_IDENTIFIER_NAME = "authentication.identifier";

    // <service/connect-options>
    private static final String SERVICE_CONNECT_OPTS_NAME = "connect-options";

    // <service/realm-name>
    private static final String SERVICE_REALM_NAME_NAME = "realm-name";

    // <security>
    private static final String SECURITY_NAME = "security";

    // <security/realm>
    private static final String SECURITY_REALM_NAME = "realm";

    // <security/realm/authentication>
    private static final String SECURITY_AUTHN_NAME = "authentication";

    // <security/realm/authentication/http-challenge-scheme>
    private static final String SECURITY_HTTP_CHALLENGE_SCHEME_NAME = "http-challenge-scheme";

    // <security/realm/authentication/authorization-mode>
    private static final String SECURITY_AUTHZ_MODE_NAME = "authorization-mode";

    // <security/realm/authentication/authorization-timeout>
    private static final String SECURITY_AUTHZ_TIMEOUT_NAME = "authorization-timeout";

    // <security/realm/authentication/login-modules>
    private static final String SECURITY_LOGIN_MODULES_NAME = "login-modules";

    public ReorderingVisitor() {
        super();
    }

    private int reorderElement(final Element parent, final String name, int newIdx) {
        Element elt = parent.getChild(name, parent.getNamespace());
        if (elt != null) {
            parent.removeContent(elt);
            parent.addContent(newIdx, elt);
            newIdx++;
        }

        return newIdx;
    }

    private int reorderElements(final Element parent, final String name, int newIdx) {

        List kids = parent.getChildren(name, parent.getNamespace());
        Iterator iter = kids.iterator();
        ListIterator<Element> listIter = toListIterator(iter);

        while (listIter.hasNext()) {
            Element kid = listIter.next();

            parent.removeContent(kid);
            parent.addContent(newIdx, kid);
            newIdx++;
        }

        return newIdx;
    }

    private void reorderService(final Element service) {
        Namespace ns = service.getNamespace();

        // Required ordering within <service>:
        // <accept>*
        // <connect>
        // <balance>
        // <type>
        // <properties>*
        // <service.domain>*
        // <encryption.key.alias>*
        // <authentication.connect>*
        // <authentication.identifier>*
        // <accept-options>
        // <connect-options>*
        // <realm-name>*
        // <authorization-constraint>
        // <mime-mapping>
        // <cross-site-constraint>
        //
        // Those marked with (*) are ones that have been possibly added by
        // earlier translation rules in the ExcaliburToMarch2012 pipeline.

        // The first 3 elements are name, description and accept options.
        // Since name and description are optional, add things in reverse
        // order at index 0 to make sure things come out correctly.
        reorderElements(service, SERVICE_ACCEPT_NAME, 0);
        reorderElements(service, SERVICE_DESCRIPTION_NAME, 0);
        reorderElements(service, SERVICE_NAME_NAME, 0);

        Element serviceProp = service.getChild(SERVICE_PROP_NAME, ns);
        if (serviceProp != null) {
            int typeIdx = getElementIndex(service, SERVICE_TYPE_NAME);
            if (typeIdx > -1) {
                service.removeContent(serviceProp);
                service.addContent(typeIdx + 1, serviceProp);
            }

            // Now, reorder the elements within the <properties> element
            int propIdx = 0;

            // <service/properties/service.domain>
            propIdx = reorderElement(serviceProp, SERVICE_PROP_SERVICE_DOMAIN_NAME, propIdx);

            // <service/properties/encryption.key.alias>
            propIdx = reorderElement(serviceProp, SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME, propIdx);

            // <service/properties/authentication.connect>
            propIdx = reorderElement(serviceProp, SERVICE_PROP_AUTHN_CONNECT_NAME, propIdx);

            // <service/properties/authentication.identifier>
            propIdx = reorderElement(serviceProp, SERVICE_PROP_AUTHN_IDENTIFIER_NAME, propIdx);
        }

        Element connectOptions = service.getChild(SERVICE_CONNECT_OPTS_NAME, ns);
        if (connectOptions != null) {
            List<String> precedents = new ArrayList<String>(3);
            precedents.add("accept-options");
            precedents.add("properties");
            precedents.add("type");
            precedents.add("balance");
            precedents.add("connect");
            precedents.add("accept");
            precedents.add("description");
            precedents.add("name");

            int idx = -1;
            for (String precedent : precedents) {
                int res = getElementIndex(service, precedent);
                if (res != -1) {
                    idx = res;
                    break;
                }
            }

            if (idx != -1) {
                service.removeContent(connectOptions);
                service.addContent(idx + 1, connectOptions);
            }
        }

        Element realmName = service.getChild(SERVICE_REALM_NAME_NAME, ns);
        if (realmName != null) {
            List<String> precedents = new ArrayList<String>(3);
            precedents.add("connect-options");
            precedents.add("accept-options");
            precedents.add("properties");
            precedents.add("type");
            precedents.add("balance");
            precedents.add("connect");
            precedents.add("accept");
            precedents.add("description");
            precedents.add("name");

            int idx = -1;
            for (String precedent : precedents) {
                int res = getElementIndex(service, precedent);
                if (res != -1) {
                    idx = res;
                    break;
                }
            }

            if (idx != -1) {
                service.removeContent(realmName);
                service.addContent(idx + 1, realmName);
            }
        }
    }

    private void reorderSecurityRealmAuthentication(final Element authn) {

        // Required ordering within <security/realm/authentication>:
        //
        // <http-challenge-scheme>*
        // <authorization-mode>*
        // <authorization-timeout>*
        // <session-timeout>
        // <login-modules>*
        //
        // Those marked with (*) are ones that have been possibly added by
        // earlier translation rules in the ExcaliburToMarch2012 pipeline.

        int newIdx = 0;

        // <security/realm/authentication/http-challenge-scheme>
        newIdx = reorderElement(authn, SECURITY_HTTP_CHALLENGE_SCHEME_NAME, newIdx);

        // <security/realm/authentication/authorization-mode>
        newIdx = reorderElement(authn, SECURITY_AUTHZ_MODE_NAME, newIdx);

        // <security/realm/authentication/authorization-timeout>
        newIdx = reorderElement(authn, SECURITY_AUTHZ_TIMEOUT_NAME, newIdx);

        // Now, just make sure that <login-modules> is last.
        newIdx = authn.getContentSize();
        reorderElement(authn, SECURITY_LOGIN_MODULES_NAME, newIdx - 1);
    }

    private void reorderSecurityRealm(final Element realm) {
        List authns = realm.getChildren();
        Iterator iter = authns.iterator();
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element kid = listIter.next();

            if (kid.getName().equals(SECURITY_AUTHN_NAME)) {
                reorderSecurityRealmAuthentication(kid);
            }
        }
    }

    private void reorderSecurity(final Element security) {
        List realms = security.getChildren();
        Iterator iter = realms.iterator();
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element kid = listIter.next();

            if (kid.getName().equals(SECURITY_REALM_NAME)) {
                reorderSecurityRealm(kid);
            }
        }
    }

    @Override
    public void visit(Element current) {

        // Visit all <service> elements, ensuring ordering...
        ElementFilter filter = new ElementFilter(SERVICE_NAME);
        Iterator serviceIter = current.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(serviceIter);

        // ... and visit each one.
        while (listIter.hasNext()) {
            Element service = listIter.next();
            reorderService(service);
        }

        // Next, visit all <security.realm> elements, ensuring ordering...
        filter = new ElementFilter(SECURITY_NAME);
        Iterator securityIter = current.getDescendants(filter);
        listIter = toListIterator(securityIter);

        // ... and visit each one.
        while (listIter.hasNext()) {
            Element security = listIter.next();
            reorderSecurity(security);
        }
    }
}
