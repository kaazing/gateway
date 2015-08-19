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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class ServiceWSConnectOptionsVisitor extends AbstractVisitor {

    private static final String SERVICE_NAME = "service";
    private static final String CONNECT_NAME = "connect";
    private static final String CONNECT_OPTS_NAME = "connect-options";
    private static final String WS_VERSION_NAME = "ws.version";
    private static final String WS_VERSION_VALUE = "draft-75";

    // RFC 3986 pattern, modified
    private static final String URI_REGEX = "^(([^:/?#]+):(//))?([^/?#]*)?([^?#]*)(\\?([^#]*))?(#(.*))?";
    private Pattern uriPattern;

    public ServiceWSConnectOptionsVisitor() {
        super();
        uriPattern = Pattern.compile(URI_REGEX);
    }

    private void visitService(final Element service) {
        ElementFilter filter = new ElementFilter(CONNECT_NAME);
        Iterator iter = service.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        while (listIter.hasNext()) {
            Element connect = listIter.next();

            // Ideally we would just use java.net.URI here. However,
            // the <connect> values might contain text like:
            //
            // ws://${gateway.hostname}:${gateway.port}/foo
            //
            // and the URI constructor will throw an exception on the '$'
            // character.
            //
            // To work around this, we use a regex and manually pull out the
            // field we want.

            Matcher matcher = uriPattern.matcher(connect.getText());
            if (matcher.matches()) {
                String scheme = matcher.group(1);

                // Trim off the '://' that the regex pulls out with the scheme
                int idx = scheme.lastIndexOf(':');
                if (idx > 0) {
                    scheme = scheme.substring(0, idx);
                }

                Namespace ns = service.getNamespace();

                if (scheme.equalsIgnoreCase("ws") || scheme.equalsIgnoreCase("wss")) {

                    // Check for an existing <connect-options> element
                    // (KG-6189).
                    ElementFilter opFilter = new ElementFilter(CONNECT_OPTS_NAME);
                    Iterator opIter = service.getDescendants(opFilter);
                    ListIterator<Element> opListIter = toListIterator(opIter);

                    Element connectOptions = null;

                    if (opListIter.hasNext()) {
                        connectOptions = opListIter.next();

                        // We have a <connect-options> already. See if
                        // it already has an <ws.version> element.
                        opFilter = new ElementFilter(WS_VERSION_NAME);
                        opIter = connectOptions.getDescendants(opFilter);
                        opListIter = toListIterator(opIter);

                        if (!opListIter.hasNext()) {
                            Element wsVersion = new Element(WS_VERSION_NAME, ns);
                            wsVersion.setText(WS_VERSION_VALUE);
                            connectOptions.addContent(wsVersion);
                        }

                    } else {
                        connectOptions = new Element(CONNECT_OPTS_NAME, ns);

                        Element wsVersion = new Element(WS_VERSION_NAME, ns);
                        wsVersion.setText(WS_VERSION_VALUE);
                        connectOptions.addContent(wsVersion);

                        service.addContent(connectOptions);
                    }

                    // We only need to inject this <connect-options> element
                    // once per <service>.
                    break;
                }
            }
        }
    }

    @Override
    public void visit(Element current) {

        // First, get all of the <service> elements...
        ElementFilter filter = new ElementFilter(SERVICE_NAME);
        Iterator iter = current.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        // ...and visit each one
        while (listIter.hasNext()) {
            Element service = listIter.next();
            visitService(service);
        }
    }
}
