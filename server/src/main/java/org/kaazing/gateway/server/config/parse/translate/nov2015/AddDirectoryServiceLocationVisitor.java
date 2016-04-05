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
package org.kaazing.gateway.server.config.parse.translate.nov2015;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

/**
 * Visitor to add "Cache-control: max-age=0" to all directory services for the 2015/11 gateway config XSD.
 *
 * Each directory service needs to contain a <location> element underneath the <properties> element:
 *     <properties>
 *       <location>
 *         <patterns>** /*< /patterns>
 *         <cache-control>max-age=0< /cache-control>
 *       < /location>
 *     < /properties>
 *
 * For more information, see:
 * https://github.com/kaazing/documentation/blob/develop/admin-reference/r_configure_gateway_service.md#cache-control-examples
 *
 */

public class AddDirectoryServiceLocationVisitor extends AbstractVisitor {

    private static final String PROPERTIES = "properties";
    private static final String DIRECTORY = "directory";
    private static final String TYPE = "type";
    private static final String SERVICE_NODE = "service";
    private static final String LOCATION = "location";
    private static final String PATTERNS = "patterns";
    private static final String CACHE_CONTROL = "cache-control";

    private static final String GENERIC_PATTERN = "**/*";
    private static final String MAX_AGE_VALUE = "max-age=0";

    private Namespace namespace;

    public AddDirectoryServiceLocationVisitor() {
        super();
    }

    @Override
    public void visit(Element element) throws Exception {
        Element typeElement = element.getChild(TYPE, namespace);
        String type = typeElement.getTextTrim();
        if (type.equalsIgnoreCase(DIRECTORY)) {
            Element location = new Element(LOCATION, namespace);

            Element patterns = new Element(PATTERNS, namespace);
            patterns.setText(GENERIC_PATTERN);
            location.addContent(patterns);

            Element cacheControl = new Element(CACHE_CONTROL, namespace);
            cacheControl.setText(MAX_AGE_VALUE);
            location.addContent(cacheControl);

            Element propertiesElement = element.getChild(PROPERTIES, namespace);
            propertiesElement.addContent(location);
        }
    }

    @SuppressWarnings("unchecked")
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
