/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.server.config.parse.translate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jdom.Document;
import org.jdom.Element;

public abstract class AbstractVisitor
        implements GatewayConfigTranslator {

    protected void copyText(final Element dst,
                            final Element src) {
        dst.setText(src.getText());
    }

    protected int getElementIndex(final Element parent,
                                  final String name) {
        int idx = -1;

        Element kid = parent.getChild(name, parent.getNamespace());
        if (kid == null) {
            return idx;
        }

        return parent.indexOf(kid);
    }

    protected ListIterator<Element> toListIterator(Iterator iter) {
        // Copy the iterated items into a List, so we can modify the
        // list while iterating over it...
        List<Element> elts = new ArrayList<>(1);
        while (iter.hasNext()) {
            elts.add((Element) iter.next());
        }

        return elts.listIterator();
    }

    public AbstractVisitor() {
    }

    // Implemented by subclasses
    public abstract void visit(Element element) throws Exception;

    @Override
    public void translate(Document dom)
            throws Exception {

        Element root = dom.getRootElement();
        visit(root);
    }
}
