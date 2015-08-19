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

import java.util.Iterator;
import java.util.ListIterator;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public abstract class AbstractManagementVisitor
    extends AbstractVisitor {

    static final String PROPERTIES_NAME = "properties";
    static final String MANAGEMENT_NAME = "management";
    static final String SERVICE_NAME = "service";

    AbstractManagementVisitor() {
        super();
    }

    protected void addService(Element serviceElement,
                              Element parent,
                              String commentText) {

        // Add the new <service> element as the last <service>, or after
        // <properties> if no <service> elements present.  And if no
        // <properties> element present, then it will be the
        // first element.

        Element precedingElement = null;

        ElementFilter filter = new ElementFilter(SERVICE_NAME);
        Iterator iter = parent.getDescendants(filter);
        ListIterator<Element> listIter = toListIterator(iter);

        while (listIter.hasNext()) {
            precedingElement = listIter.next();
        }

        if (precedingElement == null) {
            // no services, check for properties (1 only)
            filter = new ElementFilter(PROPERTIES_NAME);
            iter = parent.getDescendants(filter);
            listIter = toListIterator(iter);

            if (listIter.hasNext()) {
                precedingElement = listIter.next();
            }
        }

        int idx = 0;
        if (precedingElement != null) {
            idx = parent.indexOf(precedingElement) + 1;
        }

        parent.addContent(idx, serviceElement);

        if (commentText != null) {
            Comment comment = new Comment(commentText);
            parent.addContent(idx, comment);
        }
    }

    protected void addService(Element serviceElement,
                              Element parent) {
        addService(serviceElement, parent, null);
    }
}
