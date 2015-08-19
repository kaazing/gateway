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

public class AuthorizationModeVisitor extends AbstractVisitor {

    // <management>
    private static final String MGMT_NAME = "management";

    // <management/realm-name>
    private static final String MGMT_REALM_NAME = "realm-name";

    // <session>
    private static final String SESSION_NAME = "session";

    // <session/realm-name>
    private static final String SESSION_REALM_NAME = "realm-name";

    // <security>
    private static final String SECURITY_NAME = "security";

    // <security/realm>
    private static final String SECURITY_REALM_NAME = "realm";

    // <security/realm/name>
    private static final String SECURITY_REALM_NAME_NAME = "name";

    // <security/realm/authentication>
    private static final String SECURITY_REALM_AUTHN_NAME = "authentication";

    // <security/realm/authentication/authorization-mode>
    private static final String AUTHZ_MODE_NAME = "authorization-mode";
    private static final String AUTHZ_MODE_VALUE = "recycle";

    private List<String> knownRealms;

    public AuthorizationModeVisitor() {
        super();
        knownRealms = new ArrayList<String>();
    }

    private void visitAuthentication(Element authn) {
        List authzs = authn.getChildren(AUTHZ_MODE_NAME);

        if (authzs.size() > 0) {
            // Make sure the existing <authorization-mode> elements have
            // the correct value. Note that for 3.2 configs, this code
            // path should NOT be executed.

            Iterator iter = authzs.iterator();
            while (iter.hasNext()) {
                Element kid = (Element) iter.next();

                if (!kid.getText().equals(AUTHZ_MODE_VALUE)) {
                    kid.setText(AUTHZ_MODE_VALUE);
                }
            }

        } else {
            // Add the new <authorization-mode> element
            Element mode = new Element(AUTHZ_MODE_NAME, authn.getNamespace());
            mode.setText(AUTHZ_MODE_VALUE);
            authn.addContent(mode);
        }
    }

    private void visitRealm(Element realm) {
        Namespace ns = realm.getNamespace();

        // See whether this <security/realm/name> is one that is referenced
        // by any <session> elements.
        Element realmName = realm.getChild(SECURITY_REALM_NAME_NAME, ns);

        if (!knownRealms.contains(realmName.getText())) {
            return;
        }

        // Next, get all of the <authentication> elements...
        ElementFilter filter = new ElementFilter(SECURITY_REALM_AUTHN_NAME);
        Iterator iter = realm.getDescendants(filter);

        // If there are no <authentication> sections present, add one.
        if (!iter.hasNext()) {
            Element authn = new Element(SECURITY_REALM_AUTHN_NAME, ns);
            realm.addContent(authn);

            Iterator newIter = realm.getDescendants(filter);
            iter = newIter;
        }

        // ...and visit each one
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element authn = listIter.next();
            visitAuthentication(authn);
        }
    }

    private void visitSecurity(Element security) {
        // First, get all of the <realm> elements...
        ElementFilter filter = new ElementFilter(SECURITY_REALM_NAME);
        Iterator iter = security.getDescendants(filter);

        // ...and visit each one
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element realm = listIter.next();
            visitRealm(realm);
        }
    }

    private void visitManagement(Element mgmt) {
        Element realmName = mgmt.getChild(MGMT_REALM_NAME, mgmt.getNamespace());
        if (realmName != null) {
            knownRealms.add(realmName.getText());
        }
    }

    private void visitSession(Element session) {
        Element realmName = session.getChild(SESSION_REALM_NAME, session.getNamespace());
        if (realmName != null) {
            knownRealms.add(realmName.getText());
        }
    }

    @Override
    public void visit(Element current) {
        // First, get all of the <session> elements...
        ElementFilter filter = new ElementFilter(SESSION_NAME);
        Iterator iter = current.getDescendants(filter);

        // ...and visit each one, checking for known realms
        while (iter.hasNext()) {
            Element session = (Element) iter.next();
            visitSession(session);
        }

        // Then get all of the <management> elements...
        filter = new ElementFilter(MGMT_NAME);
        iter = current.getDescendants(filter);

        // ...and visit each one, checking for known realms
        while (iter.hasNext()) {
            Element mgmt = (Element) iter.next();
            visitManagement(mgmt);
        }

        // Next, get all of the <security> elements...
        filter = new ElementFilter(SECURITY_NAME);
        iter = current.getDescendants(filter);

        // ...and visit each one
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element security = listIter.next();
            visitSecurity(security);
        }
    }
}
