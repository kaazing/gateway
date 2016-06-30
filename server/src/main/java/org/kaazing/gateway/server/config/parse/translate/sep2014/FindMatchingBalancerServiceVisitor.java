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
package org.kaazing.gateway.server.config.parse.translate.sep2014;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

import static java.lang.String.format;

/**
 * For each balance URI on a service, make sure there is a matching balancer service accept URI.
 */
public class FindMatchingBalancerServiceVisitor extends AbstractVisitor {
    private static final String SERVICE_ELEMENT = "service";
    private static final String ACCEPT_URI_ELEMENT = "accept";
    private static final String BALANCE_URI_ELEMENT = "balance";
    private static final String TYPE_ELEMENT = "type";

    private Set<String> balanceURIs;
    private Set<String> balancerAcceptURIs;

    public FindMatchingBalancerServiceVisitor() {
        balanceURIs = new HashSet<>();
        balancerAcceptURIs = new HashSet<>();
    }

    @Override
    public void visit(Element element) throws Exception {
        // First get the balance elements from the service element (same namespace as the service element)
        List<Element> balanceElements = element.getChildren(BALANCE_URI_ELEMENT, element.getNamespace());
        if (balanceElements != null) {
            for (Element balanceElement : balanceElements) {
                balanceURIs.add(balanceElement.getValue());
            }
        }

        Element typeElement = element.getChild(TYPE_ELEMENT, element.getNamespace());

        if (typeElement == null) {
            // this will be invalid and the xsd check will throw a clean exception
            return;
        }

        String type = typeElement.getValue();
        if ("balancer".equals(type)) {
            // If there are balance elements, get the accept elements from the service element (same namespace as the service
            // element)
            List<Element> acceptElements = element.getChildren(ACCEPT_URI_ELEMENT, element.getNamespace());
            for (Element acceptElement : acceptElements) {
                balancerAcceptURIs.add(acceptElement.getValue());
            }
        }
    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        // Gather the service elements and visit them ensuring that any balance tags differ
        // from the accept tags by hostname only.
        ElementFilter nameFilter = new ElementFilter(SERVICE_ELEMENT);
        Iterator<?> iter = root.getDescendants(nameFilter);
        while (iter.hasNext()) {
            Element clusterElement = (Element) iter.next();
            visit(clusterElement);
        }

        // all the services have been visited, ensure the that every balance URI matches a balancer accept URI
        for (String balanceURI : balanceURIs) {
            if (!balancerAcceptURIs.contains(balanceURI)) {
                throw new RuntimeException(
                        format("balance URI: %s does not point to a balancer service's accept URI in the configuration file, " +
                                        "unable to launch the Gateway",
                                balanceURI));
            }
        }

        // ensure that every balancer accept URI matches a balance URI
        for (String balancerAcceptURI : balancerAcceptURIs) {
            if (!balanceURIs.contains(balancerAcceptURI)) {
                throw new RuntimeException(
                        format("Detected orphaned balancer accept URI: %s, no balance URIs in the configuration file point to " +
                                        "this balancer service.  Unable to launch the Gateway.",
                                balancerAcceptURI));
            }
        }
    }
}
