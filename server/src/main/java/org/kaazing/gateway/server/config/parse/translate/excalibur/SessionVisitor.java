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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

/**
 * For a complete description of the changes this visitor will do, see:
 *
 * https://docs.google.com/a/kaazing.com/document/d/1fTlnP7ZGUumIZVnNlkz9bYKzox79ZXIs3oocmvuIKko/edit
 *
 */
public class SessionVisitor extends AbstractVisitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionVisitor.class);

    private static final String SESSION_NAME = "session";
    private static final String SERVICE_NAME = "service";
    private static final String SECURITY_NAME = "security";

    // <session> elements
    private static final String REALM_NAME_NAME = "realm-name";
    private static final String SERVICE_DOMAIN_NAME = "service-domain";
    private static final String AUTHN_CONNECT_NAME = "authentication-connect";
    private static final String AUTHN_IDENTIFIER_NAME = "authentication-identifier";
    private static final String AUTHN_SCHEME_NAME = "authentication-scheme";
    private static final String INACTIVITY_TIMEOUT_NAME = "inactivity-timeout";
    private static final String ENCRYPTION_KEY_ALIAS_NAME = "encryption-key-alias";

    private static final String REALM_NAME = "realm";
    private static final String AUTHN_NAME = "authentication";

    // <service> elements
    private static final String ACCEPT_NAME = "accept";
    private static final String SERVICE_PROP_NAME = "properties";
    private static final String SERVICE_PROP_AUTHN_CONNECT_NAME = "authentication.connect";
    private static final String SERVICE_PROP_AUTHN_IDENTIFIER_NAME = "authentication.identifier";
    private static final String SERVICE_PROP_SERVICE_DOMAIN_NAME = "service.domain";
    private static final String SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME = "encryption.key.alias";
    private static final String MANAGEMENT_NAME = "management";

    // <security.realm.authentication> elements
    private static final String HTTP_CHALLENGE_SCHEME_NAME = "http-challenge-scheme";
    private static final String AUTHZ_TIMEOUT_NAME = "authorization-timeout";

    // RFC 3986 pattern, modified
    private static final String URI_REGEX = "^(([^:/?#]+):(//))?([^/?#]*)?([^?#]*)(\\?([^#]*))?(#(.*))?";

    private Pattern uriPattern;

    public SessionVisitor() {
        super();
        uriPattern = Pattern.compile(URI_REGEX);
    }

    private void visitSecurityRealmAuthentication(final RealmNode realmNode, final SessionNode sessionNode) {

        Element authn = realmNode.getAuthentication();
        Element session = sessionNode.getSession();
        Namespace ns = session.getNamespace();

        // Move <session.authentication-scheme> to
        // <security.realm.authentication.http-challenge-scheme> (add/remove).
        //
        // Move <session.inactivity-timeout> to
        // <security.realm.authentication.authorization-timeout> (add/remove).

        List kids = session.getChildren();
        if (kids.size() > 0) {
            Iterator iter = kids.iterator();
            ListIterator<Element> listIter = toListIterator(iter);

            while (listIter.hasNext()) {
                Element kid = listIter.next();

                if (kid.getName().equals(AUTHN_SCHEME_NAME)) {
                    Element httpChallengeScheme = new Element(HTTP_CHALLENGE_SCHEME_NAME, ns);
                    copyText(httpChallengeScheme, kid);
                    authn.addContent(httpChallengeScheme);
                    session.removeContent(kid);
                    continue;
                }

                if (kid.getName().equals(INACTIVITY_TIMEOUT_NAME)) {
                    Element authzTimeout = new Element(AUTHZ_TIMEOUT_NAME, ns);
                    copyText(authzTimeout, kid);
                    authn.addContent(authzTimeout);
                    session.removeContent(kid);
                    continue;
                }
            }
        }
    }

    private Element cloneService(final Element service, final ListIterator<Element> serviceIter) {
        Element clone = (Element) service.clone();

        // Add the cloned section to the iterator for processing on the next
        // iteration...
        serviceIter.add(clone);

        // ...AND add the cloned section to the document itself, so that it
        // shows up in the output. Add the new element just after this
        // current element.
        int idx = service.getParent().indexOf(service);
        ((Element) service.getParent()).addContent(idx + 1, clone);

        // Finally, we need to remove all of the <accept> children from the
        // clone. The <accept> elements which do NOT match the <session>
        // hostname will be added back into the clone later.
        List kids = clone.getChildren("accept", service.getNamespace());
        Iterator iter = kids.iterator();
        ListIterator<Element> listIter = toListIterator(iter);
        while (listIter.hasNext()) {
            Element kid = listIter.next();
            clone.removeContent(kid);
        }

        return clone;
    }

    private void visitService(final ServiceNode serviceNode,
                              final SessionNode sessionNode,
                              final ListIterator<Element> serviceIter) {

        Element service = serviceNode.getService();
        Element session = sessionNode.getSession();
        Namespace ns = service.getNamespace();

        // The same <session> can be associated with multiple different
        // <service> elements, hence why we do NOT want to remove any of the
        // <session> elements that we are copying into the <service> element.

        // We matched this <service> based on the <service-domain> from
        // the given <session> (sessionNode.getHostname()).
        //
        // This is where the "splitting" comes in. We need to add
        // the elements to a <service> WHERE THE <accept> elements' DOMAIN
        // MATCHES the <session.service-domain> ONLY.

        Map<String, List<Element>> serviceAccepts = serviceNode.getAccepts();

        // If there's only one entry in the map, then all <accept> elements
        // in this <service> element are in the same domain, AND that that
        // domain matches the <session.service-domain> domain. Which means
        // we do NOT need to split this <service> into different services.
        //
        // Otherwise, we clone this <service> element, and pull out the
        // non-matching <accept> elements from our source clone (and pull out
        // the matching <accept> elements from the destination clone).
        //
        // Note that we will want to add the clone to the iterator, so that
        // it can be properly processed as well.

        if (serviceAccepts.size() > 1) {
            Element clone = cloneService(service, serviceIter);

            for (Map.Entry<String, List<Element>> entry : serviceAccepts.entrySet()) {
                String acceptHostname = entry.getKey();

                // Make sure that we ignore the casing of the hostname
                if (!acceptHostname.equalsIgnoreCase(sessionNode.getHostname())) {
                    // Pull out the non-matching <accept> elements from the
                    // source <service> element, and add them to the clone.
                    List<Element> nonMatchingAccepts = entry.getValue();

                    for (Element nonMatchingAccept : nonMatchingAccepts) {
                        service.removeContent(nonMatchingAccept);
                        clone.addContent(nonMatchingAccept);
                    }
                }
            }
        }

        // Copy <session.realm-name> to <service.realm-name>
        Element serviceRealmName = new Element(REALM_NAME_NAME, ns);
        serviceRealmName.setText(sessionNode.getRealm().getText());
        service.addContent(serviceRealmName);

        // Add <service.properties>, if not already present
        Element serviceProperties = service.getChild(SERVICE_PROP_NAME, ns);
        if (serviceProperties == null) {
            serviceProperties = new Element(SERVICE_PROP_NAME, ns);
        }

        // SERVICE_PROP_AUTHN_CONNECT_NAME
        Element sessionAuthnConnect = sessionNode.getAuthenticationConnect();
        if (sessionAuthnConnect != null) {
            Element serviceAuthnConnect = new Element(SERVICE_PROP_AUTHN_CONNECT_NAME, ns);
            copyText(serviceAuthnConnect, sessionAuthnConnect);
            serviceProperties.addContent(serviceAuthnConnect);
        }

        // SERVICE_PROP_AUTHN_IDENTIFIER_NAME
        Element sessionAuthnIdentifier = sessionNode.getAuthenticationIdentifier();
        if (sessionAuthnIdentifier != null) {
            Element serviceAuthnIdentifier = new Element(SERVICE_PROP_AUTHN_IDENTIFIER_NAME, ns);
            copyText(serviceAuthnIdentifier, sessionAuthnIdentifier);
            serviceProperties.addContent(serviceAuthnIdentifier);
        }

        // SERVICE_PROP_SERVICE_DOMAIN_NAME
        Element sessionServiceDomain = sessionNode.getServiceDomain();
        if (sessionServiceDomain != null) {
            Element serviceServiceDomain = new Element(SERVICE_PROP_SERVICE_DOMAIN_NAME, ns);
            copyText(serviceServiceDomain, sessionServiceDomain);
            serviceProperties.addContent(serviceServiceDomain);
        }

        // SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME
        Element sessionEncryptionKeyAlias = sessionNode.getEncryptionKeyAlias();
        if (sessionEncryptionKeyAlias != null) {
            Element serviceEncryptionKeyAlias = new Element(SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME, ns);
            copyText(serviceEncryptionKeyAlias, sessionEncryptionKeyAlias);
            serviceProperties.addContent(serviceEncryptionKeyAlias);
        }

        if (service.getChild(SERVICE_PROP_NAME, ns) == null) {
            service.addContent(serviceProperties);
        }
    }

    private void visitManagement(ManagementNode managementNode, SessionNode sessionNode) {
        Element management = managementNode.getManagementElement();
        Element session = sessionNode.getSession();
        Namespace ns = management.getNamespace();

        // The same <session> can be associated with multiple different
        // <service> elements, hence why we do NOT want to remove any of the
        // <session> elements that we are copying into the <management> element.

        // We matched this <management> based on the <service-domain> from
        // the given <session> (sessionNode.getHostname()).
        //
        // There is no splitting in management because there can only be one
        // accept

        // Copy <session.realm-name> to <management.realm-name>
        Element managementRealmName = new Element(REALM_NAME_NAME, ns);
        managementRealmName.setText(sessionNode.getRealm().getText());
        management.addContent(managementRealmName);

        // Add <properties>, if not already present
        Element properties = management.getChild(SERVICE_PROP_NAME, ns);
        if (properties == null) {
            properties = new Element(SERVICE_PROP_NAME, ns);
        }

        // SERVICE_PROP_AUTHN_CONNECT_NAME
        Element sessionAuthnConnect = sessionNode.getAuthenticationConnect();
        if (sessionAuthnConnect != null) {
            Element serviceAuthnConnect = new Element(SERVICE_PROP_AUTHN_CONNECT_NAME, ns);
            copyText(serviceAuthnConnect, sessionAuthnConnect);
            properties.addContent(serviceAuthnConnect);
        }

        // SERVICE_PROP_AUTHN_IDENTIFIER_NAME
        Element sessionAuthnIdentifier = sessionNode.getAuthenticationIdentifier();
        if (sessionAuthnIdentifier != null) {
            Element serviceAuthnIdentifier = new Element(SERVICE_PROP_AUTHN_IDENTIFIER_NAME, ns);
            copyText(serviceAuthnIdentifier, sessionAuthnIdentifier);
            properties.addContent(serviceAuthnIdentifier);
        }

        // SERVICE_PROP_SERVICE_DOMAIN_NAME
        Element sessionServiceDomain = sessionNode.getServiceDomain();
        if (sessionServiceDomain != null) {
            Element serviceServiceDomain = new Element(SERVICE_PROP_SERVICE_DOMAIN_NAME, ns);
            copyText(serviceServiceDomain, sessionServiceDomain);
            properties.addContent(serviceServiceDomain);
        }

        // SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME
        Element sessionEncryptionKeyAlias = sessionNode.getEncryptionKeyAlias();
        if (sessionEncryptionKeyAlias != null) {
            Element serviceEncryptionKeyAlias = new Element(SERVICE_PROP_ENCRYPTION_KEY_ALIAS_NAME, ns);
            copyText(serviceEncryptionKeyAlias, sessionEncryptionKeyAlias);
            properties.addContent(serviceEncryptionKeyAlias);
        }

        if (management.getChild(SERVICE_PROP_NAME, ns) == null) {
            management.addContent(properties);
        }

    }

    @Override
    public void visit(Element current) {
        // First, get all of the <session> elements...
        ElementFilter filter = new ElementFilter(SESSION_NAME);
        Iterator sessionIter = current.getDescendants(filter);

        Map<String, SessionNode> sessionHostnames = new HashMap<String, SessionNode>(1);
        Map<String, SessionNode> sessionRealmNames = new HashMap<String, SessionNode>(1);

        // ...and visit each one
        while (sessionIter.hasNext()) {
            Element session = (Element) sessionIter.next();

            SessionNode sessionNode = new SessionNode(session);
            sessionHostnames.put(sessionNode.getHostname(), sessionNode);

            // KG-2833 Check case where session has no realm
            if (sessionNode.getRealm() == null) {
                throw new IllegalArgumentException(String.format("Realm name is required for service domain \"%s\"",
                        sessionNode.getServiceDomain()));
            }
            sessionRealmNames.put(sessionNode.getRealm().getText(), sessionNode);
        }

        // Now get all of the <security> elements associated with the
        // <realm-name> elements in the <session> nodes.

        filter = new ElementFilter(SECURITY_NAME);
        Iterator securityIter = current.getDescendants(filter);

        while (securityIter.hasNext()) {
            Element security = (Element) securityIter.next();

            SecurityNode securityNode = new SecurityNode(security);

            List<RealmNode> securityRealmNodes = securityNode.getRealms();
            if (securityRealmNodes != null) {
                for (RealmNode realmNode : securityRealmNodes) {
                    for (Map.Entry<String, SessionNode> entry : sessionRealmNames.entrySet()) {
                        if (entry.getKey().equals(realmNode.getName())) {
                            visitSecurityRealmAuthentication(realmNode, entry.getValue());
                        }
                    }
                }
            }
        }

        // Now get all of the <service> elements associated with the
        // <service-domain> elements (hostnames) in the <session> nodes.

        filter = new ElementFilter(SERVICE_NAME);
        Iterator serviceIter = current.getDescendants(filter);
        ListIterator<Element> serviceListIter = toListIterator(serviceIter);

        while (serviceListIter.hasNext()) {
            Element service = serviceListIter.next();

            ServiceNode serviceNode = new ServiceNode(uriPattern, service);

            Set<String> hostnames = serviceNode.getHostnames();
            for (String hostname : hostnames) {
                for (Map.Entry<String, SessionNode> entry : sessionHostnames.entrySet()) {

                    // Make sure that we ignore the casing of the hostname
                    if (entry.getKey().equalsIgnoreCase(hostname)) {
                        SessionNode sessionNode = entry.getValue();
                        visitService(serviceNode, sessionNode, serviceListIter);
                    }
                }
            }
        }

        // Add all session properties to management section

        filter = new ElementFilter(MANAGEMENT_NAME);
        Iterator managementIter = current.getDescendants(filter);
        ListIterator<Element> managementListIter = toListIterator(managementIter);

        while (managementListIter.hasNext()) {
            Element management = managementListIter.next();

            ManagementNode managementNode = new ManagementNode(uriPattern, management);

            String hostname = managementNode.getHostname();
            for (Map.Entry<String, SessionNode> entry : sessionHostnames.entrySet()) {
                // Make sure that we ignore the casing of the hostname
                if (entry.getKey().equalsIgnoreCase(hostname)) {
                    SessionNode sessionNode = entry.getValue();
                    visitManagement(managementNode, sessionNode);
                }
            }

        }

        // Last task: remove all the <session> elements

        filter = new ElementFilter(SESSION_NAME);
        sessionIter = current.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(sessionIter);
        while (listIter.hasNext()) {
            Element session = listIter.next();
            current.removeContent(session);
        }
    }

    // Helper classes

    // <service />
    private static class ServiceNode {
        private Element serviceElement;
        private Map<String, List<Element>> acceptHostnames;

        public ServiceNode(Pattern uriPattern, Element serviceElement) {
            this.serviceElement = serviceElement;

            // Find all of the first-level <accept> elements
            List kids = serviceElement.getChildren(ACCEPT_NAME, serviceElement.getNamespace());
            Iterator iter = kids.iterator();

            acceptHostnames = new HashMap<String, List<Element>>(1);
            while (iter.hasNext()) {
                Element accept = (Element) iter.next();

                // Ideally we would just use java.net.URI here. However,
                // the <accept> values might contain text like:
                //
                // udp://${gateway.hostname}:${gateway.port}/foo
                //
                // and the URI constructor will throw an exception on the
                // '$' character.
                //
                // To work around this, we use a regex and manually pull
                // out the field we want.

                Matcher matcher = uriPattern.matcher(accept.getText());
                if (matcher.matches()) {
                    String hostname = matcher.group(4);

                    // Trim off any possible port
                    int idx = hostname.lastIndexOf(':');
                    if (idx > 0) {
                        hostname = hostname.substring(0, idx);
                    }

                    List<Element> accepts = acceptHostnames.get(hostname);
                    if (accepts == null) {
                        accepts = new ArrayList<Element>(1);
                        acceptHostnames.put(hostname, accepts);
                    }

                    accepts.add(accept);

                } else {
                    logger.warn(String.format("Unable to extract hostname from URI '%s'", accept.getText()));
                }
            }
        }

        public Map<String, List<Element>> getAccepts() {
            return acceptHostnames;
        }

        public Set<String> getHostnames() {
            return acceptHostnames.keySet();
        }

        public Element getService() {
            return serviceElement;
        }
    }

    // <session />
    private static class SessionNode {
        private Element session;

        // <session.authentication-connect>
        private Element authnConnect;

        // <session.authentication-identifer>
        private Element authnIdentifier;

        // <session.authentication-scheme>
        private Element authnScheme;

        // <session.encryption-key-alias>
        private Element encryptionKeyAlias;

        // <session.inactivity-timeout>
        private Element inactivityTimeout;

        // <session.service-domain>
        private Element serviceDomain;
        private String hostname;

        // <session.realm-name>
        private Element realm;

        public SessionNode(Element session) {
            this.session = session;

            List kids = session.getChildren();
            if (kids.size() > 0) {
                Iterator iter = kids.iterator();
                while (iter.hasNext()) {
                    Element kid = (Element) iter.next();
                    String name = kid.getName();

                    // <realm-name>
                    if (name.equals(REALM_NAME_NAME)) {
                        setRealm(kid);

                        // <service-domain>
                    } else if (name.equals(SERVICE_DOMAIN_NAME)) {
                        setServiceDomain(kid);

                        // <inactivity-timeout>
                    } else if (name.equals(INACTIVITY_TIMEOUT_NAME)) {
                        setInactivityTimeout(kid);

                        // <authentication-connect>
                    } else if (name.equals(AUTHN_CONNECT_NAME)) {
                        setAuthenticationConnect(kid);

                        // <authentication-identifier>
                    } else if (name.equals(AUTHN_IDENTIFIER_NAME)) {
                        setAuthenticationIdentifier(kid);

                        // <authentication-scheme>
                    } else if (name.equals(AUTHN_SCHEME_NAME)) {
                        setAuthenticationScheme(kid);

                    } else if (name.equals(ENCRYPTION_KEY_ALIAS_NAME)) {
                        setEncryptionKeyAlias(kid);
                    }
                }
            }
        }

        public Element getSession() {
            return session;
        }

        public Element getAuthenticationScheme() {
            return authnScheme;
        }

        private void setAuthenticationScheme(Element authnScheme) {
            this.authnScheme = authnScheme;
        }

        public Element getServiceDomain() {
            return serviceDomain;
        }

        public String getHostname() {
            return hostname;
        }

        private void setServiceDomain(Element serviceDomain) {
            this.serviceDomain = serviceDomain;
            this.hostname = serviceDomain.getText();

            // XXX Need a GatewayConfigParser unit test for this use case!

            // According to Steve, some customers have put a leading '.'
            // in their <session.service-domain> values, since this value
            // is used for setting cookies; one HTTP cookie convention is to
            // set cookies on domain names which start with '.'.
            //
            // In order to match the domain name properly with the <service>,
            // however, we will canonicalize the domain name by removing any
            // leading '.'.
            if (this.hostname.startsWith(".")) {
                this.hostname = this.hostname.substring(1);
            }
        }

        public Element getRealm() {
            return realm;
        }

        private void setRealm(Element realm) {
            this.realm = realm;
        }

        public Element getAuthenticationConnect() {
            return authnConnect;
        }

        private void setAuthenticationConnect(Element authnConnect) {
            this.authnConnect = authnConnect;
        }

        public Element getAuthenticationIdentifier() {
            return authnIdentifier;
        }

        private void setAuthenticationIdentifier(Element authnIdentifier) {
            this.authnIdentifier = authnIdentifier;
        }

        public Element getEncryptionKeyAlias() {
            return encryptionKeyAlias;
        }

        private void setEncryptionKeyAlias(Element encryptionKeyAlias) {
            this.encryptionKeyAlias = encryptionKeyAlias;
        }

        public Element getInactivityTimeout() {
            return inactivityTimeout;
        }

        private void setInactivityTimeout(Element inactivityTimeout) {
            this.inactivityTimeout = inactivityTimeout;
        }
    }

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

    // <management>
    private static class ManagementNode {

        private Element managementElement;

        private String acceptHostname;

        public ManagementNode(Pattern uriPattern, Element managementElement) {

            this.managementElement = managementElement;

            // Find all of the first-level <accept> elements
            List kids = managementElement.getChildren(ACCEPT_NAME, managementElement.getNamespace());
            Iterator iter = kids.iterator();

            while (iter.hasNext()) {
                Element accept = (Element) iter.next();

                // Ideally we would just use java.net.URI here. However,
                // the <accept> values might contain text like:
                //
                // udp://${gateway.hostname}:${gateway.port}/foo
                //
                // and the URI constructor will throw an exception on the
                // '$' character.
                //
                // To work around this, we use a regex and manually pull
                // out the field we want.

                Matcher matcher = uriPattern.matcher(accept.getText());
                if (matcher.matches()) {
                    String hostname = matcher.group(4);

                    // Trim off any possible port
                    int idx = hostname.lastIndexOf(':');
                    if (idx > 0) {
                        hostname = hostname.substring(0, idx);
                    }

                    acceptHostname = hostname;

                } else {
                    logger.warn(String.format("Unable to extract hostname from URI '%s'", accept.getText()));
                }
            }
        }

        public Element getManagementElement() {
            return managementElement;
        }

        public String getHostname() {
            return acceptHostname;
        }

    }
}
