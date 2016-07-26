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
package org.kaazing.gateway.server.context.resolve;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.server.config.june2016.AuthenticationType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DefaultAuthenticationContext implements AuthenticationContext {
    private static final String AUTHORIZATION_MODE_CHALLENGE = "challenge";

    private static final String HTTP_CHALLENGE_SCHEME = "http-challenge-scheme";
    private static final String HTTP_HEADERS = "http-header";
    private static final String HTTP_QUERY_PARAMETERS = "http-query-parameter";
    private static final String HTTP_COOKIE_NAMES = "http-cookie";
    private static final String HTTP_AUTHORIZATION_MODE = "authorization-mode";
    private static final String SESSION_TIMEOUT = "session-timeout";

    private Map<String, String> options;

    public DefaultAuthenticationContext(AuthenticationType authenticationType) {
        options = new HashMap<>();
        parseAuthenticationType(authenticationType);
    }

    @Override
    public String getHttpChallengeScheme() {
        return options.get(HTTP_CHALLENGE_SCHEME);
    }

    @Override
    public String[] getHttpHeaders() {
        String headers = options.get(HTTP_HEADERS);
        if (headers != null) {
            return headers.split(",");
        }
        return null;
    }

    @Override
    public String[] getHttpQueryParameters() {
        String queryParams = options.get(HTTP_QUERY_PARAMETERS);
        if (queryParams != null) {
            return queryParams.split(",");
        }
        return null;
    }

    @Override
    public String[] getHttpCookieNames() {
        String cookieNames = options.get(HTTP_COOKIE_NAMES);
        if (cookieNames != null) {
            return cookieNames.split(",");
        }
        return null;
    }

    @Override
    public String getAuthorizationMode() {
        return resolveAuthorizationMode(options.get(HTTP_AUTHORIZATION_MODE));
    }

    @Override
    public String getSessionTimeout() {
        return options.get(SESSION_TIMEOUT);
    }

    @Override
    public String getProperty(String name) {
        return options.get(name);
    }

    private void parseAuthenticationType(AuthenticationType authenticationType) {
        if (authenticationType != null) {
            parseOptions(authenticationType.getDomNode(), options);
        }
    }

    private void parseOptions(Node parent, Map<String, String> optionsMap) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                NodeList content = node.getChildNodes();
                String nodeValue = "";
                for (int j = 0; j < content.getLength(); j++) {
                    Node child = content.item(j);
                    if (child != null) {
                        if (child.getNodeType() == Node.TEXT_NODE) {
                            // GatewayConfigParser skips white space so we don't need to trim. We concatenate in case
                            // the parser coughs up text content as more than one Text node.
                            String fragment = child.getNodeValue();
                            if (fragment != null) {
                                nodeValue = nodeValue + fragment;
                            }
                        }
                        // Skip over other node types
                    }
                }

                // if the option exists, convert to array
                String currentValue = optionsMap.get(node.getLocalName());
                if (currentValue != null) {
                    currentValue = format("%s,%s", currentValue, nodeValue);
                } else {
                    currentValue = nodeValue;
                }
                optionsMap.put(node.getLocalName(), currentValue);
            }
        }
    }

    private String resolveAuthorizationMode(String authorizationMode) {
        if (authorizationMode == null) {
            return AUTHORIZATION_MODE_CHALLENGE;
        } else {
            return authorizationMode;
        }
    }
}
