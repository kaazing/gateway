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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

import static java.lang.String.format;

/**
 * Compares the accept URIs to the balance URIs on a service to ensure they differ by hostname only. In order to ease
 * the transition from 4.0 to 4.1, the balanceURIs must be something that can later be promoted to the accept URIs and
 * the hostnames in the accept URIs will be moved to the http.hostname.aliases accept-option. Since only the hostname is
 * substituted as an alternate for the accept URI when doing balancing in 4.1, this visitor ensures the 4.0 is in a good
 * starting point for the rest of the 4.x code line.
 */
public class AcceptUriComparedToBalanceUriVisitor extends AbstractVisitor {

    private static final Logger logger = LoggerFactory.getLogger(AcceptUriComparedToBalanceUriVisitor.class);
    private static final String SERVICE_ELEMENT = "service";
    private static final String ACCEPT_URI_ELEMENT = "accept";
    private static final String BALANCE_URI_ELEMENT = "balance";

    // RFC 3986 pattern, modified
    private static final String URI_REGEX = "^(([^:/?#]+):(//))?([^/?#:]*)?(:)?([^/]+)([^?#]*)(\\?([^#]*))?(#(.*))?";
    private Pattern uriPattern;

    public AcceptUriComparedToBalanceUriVisitor() {
        super();
        uriPattern = Pattern.compile(URI_REGEX);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Element element) throws Exception {
        // First get the balance elements from the service element (same namespace as the service element)
        List<Element> balanceElements = element.getChildren(BALANCE_URI_ELEMENT, element.getNamespace());
        if ((balanceElements == null) || balanceElements.isEmpty()) {
            // no balance elements, no need to continue validating
            return;
        }

        // pre-process the balance elements into a map from String(scheme)->List<ParameterizedURI>
        Map<String, List<ParameterizedURI>> processedBalanceElements = new HashMap<>();
        for (Element balanceElement : balanceElements) {
            String balanceURIString = balanceElement.getValue();

            Matcher balanceMatcher = uriPattern.matcher(balanceURIString);
            if (balanceMatcher.matches()) {
                String balanceScheme = balanceMatcher.group(2);
                String balanceHost = balanceMatcher.group(4);
                String balancePort = balanceMatcher.group(6);
                String balancePath = balanceMatcher.group(7);

                ParameterizedURI pURI = new ParameterizedURI(balanceURIString, balanceScheme, balanceHost, balancePort,
                        balancePath);
                List<ParameterizedURI> pURIsForScheme = processedBalanceElements.get(balanceScheme);
                if (pURIsForScheme == null) {
                    pURIsForScheme = new LinkedList<>();
                    processedBalanceElements.put(balanceScheme, pURIsForScheme);
                }

                pURIsForScheme.add(pURI);
            } else {
                // invalid URI? throw an exception so the configuration can be cleaned up
                throw new RuntimeException(format("Unable to parse balance URI: %s", balanceURIString));
            }
        }

        // pre-process the accept elements in a map from String(scheme)->List<ParameterizedURI>
        List<Element> acceptElements = element.getChildren(ACCEPT_URI_ELEMENT, element.getNamespace());
        Map<String, List<ParameterizedURI>> processedAcceptElements = new HashMap<>();
        for (Element acceptElement : acceptElements) {
            String acceptURIString = acceptElement.getValue();
            Matcher acceptMatcher = uriPattern.matcher(acceptURIString);
            if (acceptMatcher.matches()) {
                String acceptScheme = acceptMatcher.group(2);
                String acceptHost = acceptMatcher.group(4);
                String acceptPort = acceptMatcher.group(6);
                String acceptPath = acceptMatcher.group(7);

                ParameterizedURI pURI = new ParameterizedURI(acceptURIString, acceptScheme, acceptHost, acceptPort,
                        acceptPath);
                List<ParameterizedURI> pURIsForScheme = processedAcceptElements.get(acceptScheme);
                if (pURIsForScheme == null) {
                    pURIsForScheme = new LinkedList<>();
                    processedAcceptElements.put(acceptScheme, pURIsForScheme);
                }

                pURIsForScheme.add(pURI);
            } else {
                // invalid URI? throw an exception so the configuration can be cleaned up
                throw new RuntimeException(format("Unable to parse accept URI: %s", acceptURIString));
            }
        }

        // validate that each acceptURI matches the appropriate balance URIs in all but hostname (appropriate in this
        // case
        // means "ws"
        // compares with "ws" and "wss" compares with "wss", etc.)
        for (String balanceScheme : processedBalanceElements.keySet()) {
            List<ParameterizedURI> balanceURIs = processedBalanceElements.get(balanceScheme);
            List<ParameterizedURI> acceptURIs = processedAcceptElements.get(balanceScheme);
            if (acceptURIs == null) {
                throw new RuntimeException(format("No accept URIs match balance scheme %s for balance URI %s",
                        balanceScheme, balanceURIs.iterator().next().getOriginalURI()));
            }

            for (ParameterizedURI acceptURI : acceptURIs) {
                for (ParameterizedURI balanceURI : balanceURIs) {
                    if (balanceURI.isValidBalanceTarget(acceptURI)) {
                        if (logger.isDebugEnabled()) {
                            String relationshipStr = balanceURI.getRelationshipWith(acceptURI);
                            String msg = format("Found relationship %s between balance URI: %s and accept URI: %s",
                                    relationshipStr, balanceURI.getOriginalURI(), acceptURI.getOriginalURI());

                            logger.debug(msg);
                        }
                    } else {
                        throw new RuntimeException(format(
                                "Accept URI: %s does not match balance URI %s in all but hostname.  "
                                        + "Unable to launch Gateway.", acceptURI.getOriginalURI(),
                                balanceURI.getOriginalURI()));
                    }
                }
            }
        }

        // validate that each balanceURI matches the appropriate acceptURIs in all but hostname (appropriate in this
        // case means
        // "ws"
        // compares with "ws" and "wss" compares with "wss", etc.). This is the second pass through as a two-way match
        // is
        // required.
        // The first is to check that for each balance scheme present on the service there are matching accepts. The
        // second is
        // for
        // each accept scheme present on the service there are matching balance tags.
        for (String acceptScheme : processedAcceptElements.keySet()) {
            List<ParameterizedURI> acceptURIs = processedAcceptElements.get(acceptScheme);
            List<ParameterizedURI> balanceURIs = processedBalanceElements.get(acceptScheme);
            if (balanceURIs == null) {
                throw new RuntimeException(format("No balance URIs match accept scheme %s for accept URI %s",
                        acceptScheme, acceptURIs.iterator().next().getOriginalURI()));
            }

            // No need to iterate over all accept URIs, that work was done in the loop above. This loop is just to
            // validate
            // that all accept URI schemes have balance URIs. If they do, then the match was validated above.
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
    }

    private final class ParameterizedURI {
        private final String originalURI;
        private final String scheme;
        private final String host;
        private final String port;
        private final String path;

        private ParameterizedURI(String originalURI, String scheme, String host, String port, String path) {
            this.originalURI = originalURI;
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.path = path;
        }

        private String getOriginalURI() {
            return originalURI;
        }

        private String getScheme() {
            return this.scheme;
        }

        private String getHost() {
            return this.host;
        }

        private String getPort() {
            return this.port;
        }

        private String getPath() {
            return this.path;
        }

        public boolean isValidBalanceTarget(ParameterizedURI balanceTarget) {
            boolean matches = scheme.equals(balanceTarget.getScheme());
            matches = matches && port.equals(balanceTarget.getPort());
            matches = matches && path.equals(balanceTarget.getPath());
            matches = matches && !host.equals(balanceTarget.getHost()); // Make sure the hosts aren't exactly the same

            return matches;
        }

        public String getRelationshipWith(ParameterizedURI otherURI) {
            String otherHost = otherURI.getHost();
            if (!host.startsWith("${") && !otherHost.startsWith("${")) {
                // subdomain
                if (otherHost.endsWith("." + host)) {
                    return "SUBDOMAIN";
                } else {
                    // check for PEER domain
                    int firstDotInHost = host.indexOf('.');
                    int firstDotInOther = otherHost.indexOf('.');
                    if ((firstDotInHost != -1) && (firstDotInOther != -1)) {
                        String remainingHostDomain = host.substring(firstDotInHost);
                        String remainingOtherDomain = otherHost.substring(firstDotInOther);
                        if (remainingHostDomain.equals(remainingOtherDomain)) {
                            return "PEERDOMAIN";
                        }
                    }
                }
            }

            // if it didn't match sub or peer domain, just return ANY
            return "ANY";
        }
    }
}
