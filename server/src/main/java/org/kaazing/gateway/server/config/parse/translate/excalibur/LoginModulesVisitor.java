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

import java.util.Iterator;
import java.util.ListIterator;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class LoginModulesVisitor extends AbstractVisitor {

    // <security>
    private static final String SECURITY_NAME = "security";

    // <security/realm>
    private static final String SECURITY_REALM_NAME = "realm";

    // <security/realm/authentication>
    private static final String SECURITY_REALM_AUTHN_NAME = "authentication";

    // <security/realm/authentication/http-challenge-scheme>
    private static final String HTTP_CHALLENGE_SCHEME_NAME = "http-challenge-scheme";
    private static final String HTTP_CHALLENGE_SCHEME_VALUE = "Application Basic";

    // <security/realm/authentication/login-modules>
    private static final String LOGIN_MODULES_NAME = "login-modules";

    // <security/realm/authentication/login-module>
    private static final String LOGIN_MODULE_NAME = "login-module";

    public LoginModulesVisitor() {
        super();
    }

    private void visitAuthentication(final Element authn, final Element realm) {

        // First, make sure we have a <login-modules> element.
        ElementFilter filter = new ElementFilter(LOGIN_MODULES_NAME);
        Iterator iter = authn.getDescendants(filter);

        Element loginModules = null;

        // If there are no <login-modules> sections present, add one.
        if (!iter.hasNext()) {
            loginModules = new Element(LOGIN_MODULES_NAME, authn.getNamespace());
            authn.addContent(loginModules);

        } else {
            loginModules = (Element) iter.next();
        }

        // Next, move (remove/add) any <login-module> from parent <realm>
        // into <login-modules>.

        filter = new ElementFilter(LOGIN_MODULE_NAME);
        iter = realm.getDescendants(filter);

        ListIterator<Element> listIter = toListIterator(iter);

        while (listIter.hasNext()) {
            Element loginModule = listIter.next();
            listIter.remove();
            realm.removeContent(loginModule);

            loginModules.addContent(loginModule);
        }
    }

    private void visitRealm(final Element realm) {
        // First, get all of the <authentication> elements...
        ElementFilter filter = new ElementFilter(SECURITY_REALM_AUTHN_NAME);
        Iterator iter = realm.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        // If there are no <authentication> sections present, add one.
        if (!listIter.hasNext()) {
            Namespace ns = realm.getNamespace();

            Element authn = new Element(SECURITY_REALM_AUTHN_NAME, ns);

            // And add the <http-challenge-scheme> element, too.
            Element httpChallengeScheme = new Element(HTTP_CHALLENGE_SCHEME_NAME, ns);
            httpChallengeScheme.setText(HTTP_CHALLENGE_SCHEME_VALUE);
            authn.addContent(httpChallengeScheme);

            realm.addContent(authn);
            listIter.add(authn);
        }

        iter = realm.getDescendants(filter);
        listIter = toListIterator(iter);

        // ...and visit each one
        while (listIter.hasNext()) {
            Element authn = listIter.next();
            visitAuthentication(authn, realm);
        }
    }

    private void visitSecurity(final Element security) {
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

    @Override
    public void visit(Element current) {
        // First, get all of the <security> elements...
        ElementFilter filter = new ElementFilter(SECURITY_NAME);
        Iterator iter = current.getDescendants(filter);

        // ...and visit each one
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element security = listIter.next();
            visitSecurity(security);
        }
    }
}
