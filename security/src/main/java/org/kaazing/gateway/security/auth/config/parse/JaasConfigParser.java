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
package org.kaazing.gateway.security.auth.config.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.kaazing.gateway.security.auth.config.JaasConfig;
import org.kaazing.gateway.util.parse.ConfigParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class JaasConfigParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaasConfigParser.class);

    public JaasConfig parse(URL resource) throws Exception {
        List<String> errors = new ArrayList<>();
        JaasConfig result = parse0(resource, new JaasConfigHandler(errors));
        if  (!errors.isEmpty()) {
            throw new ParserConfigurationException(errors.get(0));
        }
        return result;
    }


    private JaasConfig parse0(URL resource, JaasConfigHandler handler)
            throws IOException, ParserConfigurationException, SAXException {
        InputStream in = resource.openStream();
        try {
            return parse0(in, handler);
        } finally {
            in.close();
        }
    }

    private JaasConfig parse0(InputStream in, JaasConfigHandler handler)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(in, handler);
        return handler.getAuthConfig();
    }

    private enum HandlerState {
        DOCUMENT, JAAS_CONFIG,
        USER, USER_NAME, USER_PASSWORD, USER_ROLE_NAME,
        ROLE, ROLE_NAME, ROLE_DESCRIPTION, ROLE_ROLE_NAME }

    private static class JaasConfigHandler extends DefaultHandler {

        private static final String NAMESPACE_URI = "http://xmlns.kaazing.com/jaas-config/centurion";
        private static final String BATTLESTAR_URI = "http://xmlns.kaazing.org/jaas-config/battlestar";
        private static final String ATLANTIS_URI = "http://xmlns.kaazing.org/jaas-config/atlantis";

        private static final String JAAS_CONFIG_ELEMENT = "jaas-config";
        private static final String USER_ELEMENT = "user";
        private static final String ROLE_ELEMENT = "role";
        private static final String NAME_ELEMENT = "name";
        private static final String PASSWORD_ELEMENT = "password";
        private static final String DESCRIPTION_ELEMENT = "description";
        private static final String ROLE_NAME_ELEMENT = "role-name";
        private static final Properties EMPTY_PROPERTIES = new Properties();
        private static  List<String> errors = new ArrayList<>();

        private final File configFile;
        private HandlerState handlerState;
        private Deque<Object> handlerStack;
        private DefaultJaasConfig authConfig;

        public JaasConfigHandler(List<String> errors) {
            this(null, errors);
        }

        public JaasConfigHandler(File configFile, List<String> errors) {
            handlerStack = new ArrayDeque<>(4);
            this.configFile = configFile;
        }

        public JaasConfig getAuthConfig() {
            return authConfig;
        }

        @Override
        public void startDocument() throws SAXException {
            handlerState = HandlerState.DOCUMENT;
        }

        @Override
        public void endDocument() throws SAXException {
            if (handlerState != HandlerState.DOCUMENT) {
                throw new SAXException("Invalid handler state: " + handlerState);
            }

            if (!handlerStack.isEmpty()) {
                throw new SAXException("Handler stack not empty: " + handlerStack);
            }
        }

        @Override
        public void startElement(String uri, String localName, String name,
                Attributes attributes) throws SAXException {
            if (NAMESPACE_URI.equals(uri)) {
                switch (handlerState) {
                case DOCUMENT:
                    if (JAAS_CONFIG_ELEMENT.equals(name)) {
                        handlerStack.push(new DefaultJaasConfig());
                        handlerState = HandlerState.JAAS_CONFIG;
                        return;
                    }
                    break;
                case JAAS_CONFIG:
                    if (USER_ELEMENT.equals(name)) {
                        handlerStack.push(new DefaultUserConfig());
                        handlerState = HandlerState.USER;
                        return;
                    }
                    else if (ROLE_ELEMENT.equals(name)) {
                        handlerStack.push(new DefaultRoleConfig());
                        handlerState = HandlerState.ROLE;
                        return;
                    }
                    break;
                case USER:
                    if (NAME_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.USER_NAME;
                        return;
                    }
                    else if (PASSWORD_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.USER_PASSWORD;
                        return;
                    }
                    else if (ROLE_NAME_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.USER_ROLE_NAME;
                        return;
                    }
                    break;
                case ROLE:
                    if (NAME_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.ROLE_NAME;
                        return;
                    }
                    else if (DESCRIPTION_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.ROLE_DESCRIPTION;
                        return;
                    }
                    else if (ROLE_NAME_ELEMENT.equals(name)) {
                        handlerStack.push(new StringBuilder());
                        handlerState = HandlerState.ROLE_ROLE_NAME;
                        return;
                    }
                    break;
                }
            }
            else if (ATLANTIS_URI.equals(uri) || BATTLESTAR_URI.equals(uri)) {
                if (configFile != null) {
                    LOGGER.error("Update your configuration namespace in the file \"" +
                            configFile.getAbsolutePath() + "\" to use \"" + NAMESPACE_URI + "\"");
                }
                else {
                    LOGGER.error("Update your configuration namespace to use \"" + NAMESPACE_URI + "\"");
                }
            }

            throw new SAXException("Unexpected element: {" + uri + "} " + name);
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            switch (handlerState) {
            case DOCUMENT:
            case JAAS_CONFIG:
            case USER:
            case ROLE:
                break;
            case USER_NAME:
            case USER_PASSWORD:
            case USER_ROLE_NAME:
            case ROLE_NAME:
            case ROLE_DESCRIPTION:
            case ROLE_ROLE_NAME:
                String string = new StringBuilder().append(ch, start, length).toString();
                string = ConfigParameter.resolveAndReplace(ch, start, length, Collections.emptyMap(),
                        EMPTY_PROPERTIES, errors);
                StringBuilder sb = (StringBuilder) handlerStack.peek();
                sb.append(string);
                break;
            default:
                throw new SAXException("Unexpected handler state: " + handlerState);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            switch (handlerState) {
            case JAAS_CONFIG: {
                this.authConfig = (DefaultJaasConfig) handlerStack.pop();
                handlerState = HandlerState.DOCUMENT;
                break;
            }
            case USER: {
                DefaultUserConfig userConfig = (DefaultUserConfig) handlerStack.pop();
                DefaultJaasConfig authConfig = (DefaultJaasConfig) handlerStack.peek();
                authConfig.getUsers().put(userConfig.getName(), userConfig);
                handlerState = HandlerState.JAAS_CONFIG;
                break;
            }
            case USER_NAME: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultUserConfig userConfig = (DefaultUserConfig) handlerStack.peek();
                userConfig.setName(sb.toString());
                handlerState = HandlerState.USER;
                break;
            }
            case USER_PASSWORD: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultUserConfig userConfig = (DefaultUserConfig) handlerStack.peek();
                userConfig.setPassword(sb.toString());
                handlerState = HandlerState.USER;
                break;
            }
            case USER_ROLE_NAME: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultUserConfig userConfig = (DefaultUserConfig) handlerStack.peek();
                userConfig.getRoleNames().add(sb.toString());
                handlerState = HandlerState.USER;
                break;
            }
            case ROLE: {
                DefaultRoleConfig roleConfig = (DefaultRoleConfig) handlerStack.pop();
                DefaultJaasConfig authConfig = (DefaultJaasConfig) handlerStack.peek();
                authConfig.getRoles().put(roleConfig.getName(), roleConfig);
                handlerState = HandlerState.JAAS_CONFIG;
                break;
            }
            case ROLE_NAME: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultRoleConfig roleConfig = (DefaultRoleConfig) handlerStack.peek();
                roleConfig.setName(sb.toString());
                handlerState = HandlerState.ROLE;
                break;
            }
            case ROLE_DESCRIPTION: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultRoleConfig roleConfig = (DefaultRoleConfig) handlerStack.peek();
                roleConfig.setDescription(sb.toString());
                handlerState = HandlerState.ROLE;
                break;
            }
            case ROLE_ROLE_NAME: {
                StringBuilder sb = (StringBuilder) handlerStack.pop();
                DefaultRoleConfig roleConfig = (DefaultRoleConfig) handlerStack.peek();
                roleConfig.getRoleNames().add(sb.toString());
                handlerState = HandlerState.ROLE;
                break;
            }
            }
        }
    }
}
