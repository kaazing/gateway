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

package org.kaazing.gateway.server.config.parse.translate.june2016;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class RemoveRealmVisitor extends AbstractVisitor {
    
    private static final String REALM = "realm-name";
    private static final String CONSTRAINT = "auth-constraint";
    private static final String AUTH_CONSTRAINT = "authorization-constraint";
    private static final String SERVICE_NODE = "service";
    
    private Namespace namespace;
    
    public RemoveRealmVisitor() {
        super();
    }

    @Override
    public void visit(Element element) throws Exception {
        Element typeElement = element.getChild(REALM, namespace);
        if (typeElement != null) {
            if (element.getChildren(CONSTRAINT, namespace).size() == 0 && element.getChildren(AUTH_CONSTRAINT, namespace).size() == 0) {
                element.removeChild(REALM, namespace);
            }
        }   
    }
    
    @Override
    public void translate(Document dom) throws Exception {
        Element root = dom.getRootElement();
        namespace = root.getNamespace();
        List<Element> children = dom.getRootElement().getChildren(SERVICE_NODE, namespace);
        for (Element child : children) {
            visit(child);
        }
    }
}
