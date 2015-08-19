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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;

import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class McpAcceptConnectToUdpAcceptConnectTranslator extends AbstractVisitor {

    Logger logger = Launcher.getGatewayStartupLogger();

    private static final String SERVICE_ELEMENT = "service";
    private static final String ACCEPT_ELEMENT = "accept";
    private static final String ACCEPT_OPTIONS_ELEMENT = "accept-options";
    private static final String CONNECT_ELEMENT = "connect";
    private static final String CONNECT_OPTIONS_ELEMENT = "connect-options";

    @Override
    public void visit(Element element) throws Exception {

        // Step 1 identify accept/connect elements
        List<Element> acceptElements = element.getChildren(ACCEPT_ELEMENT, element.getNamespace());
        List<Element> connectElements = element.getChildren(CONNECT_ELEMENT, element.getNamespace());

        // Step 2 identify accept elements that have mcp uri and switch to udp
        String acceptUDPInterface = convertMcpURIsToUDPUris(acceptElements, ACCEPT_ELEMENT);

        // Step 3 identify connect elements that have mcp uri and switch to udp
        String connectUDPInterface = convertMcpURIsToUDPUris(connectElements, CONNECT_ELEMENT);

        // Step 4 add accept-options udp interface
        if (acceptUDPInterface != null) {
            addUdpInterface(element, ACCEPT_OPTIONS_ELEMENT, acceptUDPInterface);
        }
        // Step 5 add connect-options udp interface
        if (connectUDPInterface != null) {
            addUdpInterface(element, CONNECT_OPTIONS_ELEMENT, connectUDPInterface);
        }
    }

    /**
     * Converts Mcp Uri elements to UDP
     * @param uriElements
     * @return udp interface to add
     */
    private String convertMcpURIsToUDPUris(List<Element> uriElements, String acceptOrConnect) {
        String deviceToUse = null;
        for (Element uriElement : uriElements) {
            String uri = uriElement.getText();
            String originalUri = uri;

            // if multicast
            if (uri.startsWith("mcp")) {

                // replace udp with mcp
                uri = uri.replaceFirst("mcp", "udp");

                Pattern genericPattern = Pattern.compile("udp://(?<host>[\\w\\.\\:]+)");
                Pattern groupPattern = Pattern
                        .compile("udp://(?<host>[\\w\\.]+)@(?<device>[\\w\\.]+):(?<port>[\\w\\.]+)");
                Matcher matcher = groupPattern.matcher(uri);

                // if has device host name, use it to set udp.interface
                if (matcher.matches()) {
                    // turn udp://multicast-group@device-host-name:port-number into udp://multicast-group:port-number
                    // and retain device name

                    String deviceName = matcher.group("device");

                    // convert deviceHostname to deviceName
                    try {
                        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress
                                .getByName(deviceName));
                        deviceName = networkInterface.getDisplayName();
                    } catch (SocketException e) {
                        // NOOP
                    } catch (UnknownHostException e) {
                        // NOOP
                    }

                    // set it as the device to use if it doesn't conflict
                    if (deviceToUse == null) {
                        deviceToUse = deviceName;
                    } else if (!deviceToUse.equals(deviceName)) {
                        // if it does conflict with existing accept or connect option through an exception
                        throw new RuntimeException(
                                String.format(
                                    "Different udp.interface detected for multicast on %s, one using %s and another using %s. "
                                       + "Seperate the multicasts addresses into different services or have them use the "
                                       + "same network interface/hostname", acceptOrConnect, deviceToUse,
                                        deviceName));
                    }
                    uri = String.format("udp://%s:%s", matcher.group("host"), matcher.group("port"));
                } else {
                    // else if it doesn't have a interface specified choose one
                    if (deviceToUse == null) {
                        // If no network interface specified on an already parsed mcp address then choose localhost
                        try {
                            deviceToUse = NetworkInterface.getByInetAddress(InetAddress.getByName("localhost"))
                                    .getDisplayName();
                        } catch (SocketException e) {
                            // should never get here, but if we do we will set it as null in the config and then throw
                            // an exception latter when we try to use it
                        } catch (UnknownHostException e) {
                            // should never get here, but if we do we will set it as null in the config and then throw
                            // an exception latter when we try to use it
                        }
                    }
                    // if no device specified log a warning
                    logger.warn(String
                        .format("The udp.interface was not specified for %s on %s, specify the interface is required "
                         + "for multicast in newest version of the gateway as otherwise it would be randomly "
                         + "selected, filling in the udp.interface with %s", originalUri, acceptOrConnect,
                                    deviceToUse));
                }
                assert deviceToUse != null;

                // Set the uriElement with the new uri
                uriElement.setText(uri);
            }
        }
        return deviceToUse;
    }

    /**
     * Adds a udp.interface to the element specified, creating it if it doesn't exist
     * @param connectOptionsElement
     * @param udpInterface
     */
    private void addUdpInterface(Element parent, String elementNameToAddTo, String udpInterface) {
        // if options exists reuse it else create it
        Element options = parent.getChild(elementNameToAddTo, parent.getNamespace());
        if (options == null) {
            options = new Element(elementNameToAddTo, parent.getNamespace());
            parent.addContent(options);
        }
        Element udpInterfaceElement = new Element("udp.interface", parent.getNamespace());
        udpInterfaceElement.setText(udpInterface);
        options.addContent(udpInterfaceElement);

    }

    @Override
    public void translate(Document dom) throws Exception {

        Element root = dom.getRootElement();

        // Visit every service element
        ElementFilter nameFilter = new ElementFilter(SERVICE_ELEMENT);
        Iterator<?> iter = root.getDescendants(nameFilter);

        // to get around concurrent modification
        List<Element> children = new ArrayList<Element>();
        while (iter.hasNext()) {
            Element child = (Element) iter.next();
            children.add(child);
        }

        for (Element child : children) {
            visit(child);
        }
    }
}
