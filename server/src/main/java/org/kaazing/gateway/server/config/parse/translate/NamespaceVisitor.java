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

import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets the namespace on the given DOM to be the given
 * {@ GatewayConfigNamespace}.
 */
public class NamespaceVisitor extends AbstractVisitor {

    private static final Logger logger = LoggerFactory.getLogger(NamespaceVisitor.class);

    private GatewayConfigNamespace ns;

    public NamespaceVisitor(GatewayConfigNamespace ns) {
        super();
        this.ns = ns;
    }

    @Override
    public void visit(Element current) {
        current.setNamespace(Namespace.getNamespace("", ns.toURI()));

        List<?> kids = current.getChildren();
        if (kids.size() > 0) {
            Iterator<?> iter = kids.iterator();
            while (iter.hasNext()) {
                Element kid = (Element) iter.next();
                visit(kid);
            }
        }
    }

    @Override
    public void translate(Document dom) throws Exception {

        super.translate(dom);

        // In order to change the namespace of the document, we have to
        // manually walk the DOM and change the namespace of EACH ELEMENT.
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Set XML namespace to URI '%s'", ns.toURI()));
        }
    }
}
