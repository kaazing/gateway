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

import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

public class RenameElementVisitor extends AbstractVisitor {

    private String originalName;
    private String newName;

    public RenameElementVisitor() {
        super();
    }

    public RenameElementVisitor(final String originalName, final String newName) {
        super();
        setOriginalName(originalName);
        setNewName(newName);
    }

    public void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    public void setNewName(final String newName) {
        this.newName = newName;
    }

    @Override
    public void visit(Element current) {
        current.setName(newName);
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        ElementFilter nameFilter = new ElementFilter(originalName);
        Iterator iter = root.getDescendants(nameFilter);

        while (iter.hasNext()) {
            Element elt = (Element) iter.next();
            visit(elt);
        }
    }
}
