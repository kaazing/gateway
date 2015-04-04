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

package org.kaazing.gateway.management.config;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.context.DefaultManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.Utils;

public class ServiceConfigurationBeanImpl implements ServiceConfigurationBean {

    //    private static final AtomicInteger serviceConfigurationIds = new AtomicInteger(0);
    private final ServiceContext serviceContext;
    private final GatewayManagementBean gatewayBean;
    private final int id;

    public ServiceConfigurationBeanImpl(ServiceContext serviceContext, GatewayManagementBean gatewayBean) {
        this.serviceContext = serviceContext;
        this.gatewayBean = gatewayBean;
        this.id = DefaultManagementContext.getNextServiceIndex(serviceContext);
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayBean;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getType() {
        return serviceContext.getServiceType();
    }

    @Override
    public String getServiceName() {
        String name = serviceContext.getServiceName();
        return name == null ? "" : name;
    }

    @Override
    public String getServiceDescription() {
        String desc = serviceContext.getServiceDescription();
        return desc == null ? "" : desc;
    }

    @Override
    public String getAccepts() {
        if (serviceContext.supportsAccepts()) {
            Collection<URI> accepts = serviceContext.getAccepts();
            return accepts == null ? "" : new JSONArray(accepts).toString();
        } else {
            return null;
        }
    }

    @Override
    public String getAcceptOptions() {
        if (serviceContext.supportsAccepts()) {

            AcceptOptionsContext context = serviceContext.getAcceptOptionsContext();
            Map<String, Object> acceptOptions = context.asOptionsMap();

            JSONObject jsonOptions = new JSONObject();
            JSONObject jsonObj;

            try {
                if (context != null) {
                    Map<String, String> binds = context.getBinds();
                    if ((binds != null) && !binds.isEmpty()) {
                        jsonObj = new JSONObject();
                        for (String key : binds.keySet()) {
                            jsonObj.put(key, binds.get(key));
                        }
                        jsonOptions.put("binds", jsonObj);
                    }

                    String[] sslCiphers = (String[]) acceptOptions.get("ssl.ciphers");
                    if (sslCiphers != null) {
                        String cipherString = Utils.asCommaSeparatedString(asList(sslCiphers));
                        if (cipherString != null && cipherString.length() > 0) {
                            jsonOptions.put("ssl.ciphers", cipherString);
                        }
                    }

                    boolean isSslEncryptionEnabled = (Boolean) acceptOptions.get("ssl.encryption");
                    jsonOptions.put("ssl.encryption",
                            isSslEncryptionEnabled ? "enabled" : "disabled");

                    boolean wantClientAuth = (Boolean) acceptOptions.get("ssl.wantClientAuth");
                    boolean needClientAuth = (Boolean) acceptOptions.get("ssl.needClientAuth");
                    if (needClientAuth) {
                        jsonOptions.put("ssl.verify-client", "required");
                    } else if (wantClientAuth) {
                        jsonOptions.put("ssl.verify-client", "optional");
                    } else {
                        jsonOptions.put("ssl.verify-client", "none");
                    }


                    // NOTE: we do NOT (at least in 4.0) show the WS extensions
                    // or WS protocols to users (Command Center or otherwise), so don't send them out.
//                    List<String> wsExtensions = context.getWsExtensions();
//                    if ((wsExtensions != null) && !wsExtensions.isEmpty()) {
//                        jsonArray = new JSONArray();
//                        for (String wsExtension : wsExtensions) {
//                            jsonArray.put(wsExtension);
//                        }
//                        jsonOptions.put("ws-extensions", jsonArray);
//                    }
//                  List<String> wsProtocols = context.getWsProtocols();
//                  if ((wsProtocols != null) && !wsProtocols.isEmpty()) {
//                      jsonArray = new JSONArray();
//                      for (String wsProtocol : wsProtocols) {
//                          jsonArray.put(wsProtocol);
//                      }
//                      jsonOptions.put("ws-protocols", jsonArray);
//                  }


                    jsonOptions.put("ws.maximum.message.size", (Long) acceptOptions.get("ws.maximum.message.size"));

                    Long wsInactivityTimeout = (Long) acceptOptions.get("ws.inactivity.timeout");
                    if (wsInactivityTimeout != null) {
                        jsonOptions.put("ws.inactivity.timeout", wsInactivityTimeout);
                    }

                    Integer httpKeepAlive = (Integer) acceptOptions.get("http.keepalive.timeout");
                    if (httpKeepAlive != null) {
                        jsonOptions.put("http.keepalive.timeout", httpKeepAlive);
                    }

                    URI pipeTransport = (URI) acceptOptions.get("pipe.transport");
                    if (pipeTransport != null) {
                        jsonOptions.put("pipe.transport", pipeTransport.toString());
                    }

                    URI tcpTransport = (URI) acceptOptions.get("tcp.transport");
                    if (tcpTransport != null) {
                        jsonOptions.put("tcp.transport", tcpTransport.toString());
                    }

                    URI sslTransport = (URI) acceptOptions.get("ssl.transport");
                    if (sslTransport != null) {
                        jsonOptions.put("ssl.transport", sslTransport.toString());
                    }

                    URI httpTransport = (URI) acceptOptions.get("http.transport");
                    if (httpTransport != null) {
                        jsonOptions.put("http.transport", httpTransport.toString());
                    }

                    long tcpMaxOutboundRate = (Long) acceptOptions.get("tcp.maximum.outbound.rate");
                    jsonOptions.put("tcp.maximum.outbound.rate", tcpMaxOutboundRate);
                }
            } catch (Exception ex) {
                // This is only for JSON exceptions, but there should be no way to
                // hit this.
            }

            return jsonOptions.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getBalances() {
        Collection<URI> balances = serviceContext.getBalances();
        return balances == null ? "" : new JSONArray(balances).toString();
    }

    @Override
    public String getConnects() {
        if (serviceContext.supportsConnects()) {
            Collection<URI> connects = serviceContext.getConnects();
            return connects == null ? "" : new JSONArray(connects).toString();
        } else {
            return null;
        }
    }

    @Override
    public String getConnectOptions() {
        if (serviceContext.supportsConnects()) {
            ConnectOptionsContext context = serviceContext.getConnectOptionsContext();
            Map<String, Object> connectOptions = context.asOptionsMap();

            JSONObject jsonOptions = new JSONObject();

            try {
                if (context != null) {

                    String[] sslCiphersArray = (String[]) connectOptions.get("ssl.ciphers");
                    if (sslCiphersArray != null) {
                        List<String> sslCiphers = Arrays.asList(sslCiphersArray);
                        if (sslCiphers.size() > 0) {
                            jsonOptions.put("ssl.ciphers", sslCiphers);
                        }
                    }

                    // NOTE: we do NOT (at least in 4.0) show the WS extensions
                    // or WS protocols to users (Command Center or otherwise), so don't send them out.
                    //WebSocketWireProtocol protocol = connectOptions.getWebSocketWireProtocol();
                    //sb.append("websocket-wire-protocol=" + protocol);

                    String wsVersion = (String) connectOptions.get("ws.version");
                    if (wsVersion != null) {
                        jsonOptions.put("ws.version", wsVersion);
                    }

                    URI pipeTransport = (URI) connectOptions.get("pipe.transport");
                    if (pipeTransport != null) {
                        jsonOptions.put("pipe.transport", pipeTransport.toString());
                    }

                    URI tcpTransport = (URI) connectOptions.get("tcp.transport");
                    if (tcpTransport != null) {
                        jsonOptions.put("tcp.transport", tcpTransport.toString());
                    }

                    URI sslTransport = (URI) connectOptions.get("ssl.transport");
                    if (sslTransport != null) {
                        jsonOptions.put("ssl.transport", sslTransport.toString());
                    }

                    URI httpTransport = (URI) connectOptions.get("http.transport");
                    if (httpTransport != null) {
                        jsonOptions.put("http.transport", httpTransport.toString());
                    }
                }
            } catch (Exception ex) {
                // This is only for JSON exceptions, but there should be no way to
                // hit this.
            }

            return jsonOptions.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getCrossSiteConstraints() {
        Map<URI, ? extends Map<String, ? extends CrossSiteConstraintContext>> crossSiteConstraints =
                serviceContext.getCrossSiteConstraints();

        JSONArray jsonConstraints = new JSONArray();

        if ((crossSiteConstraints != null) && !crossSiteConstraints.isEmpty()) {
            Collection<? extends Map<String, ? extends CrossSiteConstraintContext>> crossSiteConstraintsValues =
                    crossSiteConstraints.values();
            if ((crossSiteConstraintsValues != null) && !crossSiteConstraintsValues.isEmpty()) {
                Map<String, ? extends CrossSiteConstraintContext> constraintMap = crossSiteConstraintsValues.iterator().next();
                Collection<? extends CrossSiteConstraintContext> constraints = constraintMap.values();
                for (CrossSiteConstraintContext constraint : constraints) {
                    JSONObject jsonObj = new JSONObject();

                    String allowHeaders = constraint.getAllowHeaders();
                    String allowMethods = constraint.getAllowMethods();
                    String allowOrigin = constraint.getAllowOrigin();
                    Integer maxAge = constraint.getMaximumAge();

                    try {
                        jsonObj.put("allow-origin", allowOrigin);
                        jsonObj.put("allow-methods", allowMethods);

                        if (allowHeaders != null) {
                            jsonObj.put("allow-headers", allowHeaders);
                        }
                        if (maxAge != null) {
                            jsonObj.put("maximum-age", maxAge);
                        }

                        jsonConstraints.put(jsonObj);
                    } catch (Exception ex) {
                        // It is a programming error to get to here. We should never
                        // get here, because we're just adding strings above.
                    }
                }
            }
        }

        return jsonConstraints.toString();
    }

    @Override
    public String getMimeMappings() {
        if (serviceContext.supportsMimeMappings()) {
            Map<String, String> mimeMappings = serviceContext.getMimeMappings();
            return mimeMappings == null ? "" : new JSONObject(mimeMappings).toString();
        } else {
            return null;
        }
    }

    @Override
    public String getProperties() {
        ServiceProperties properties = serviceContext.getProperties();
        return properties == null ? "" : asJSONObject(properties).toString();
    }

    @Override
    public String getRequiredRoles() {
        Collection<String> roles = asList(serviceContext.getRequireRoles());
        return roles == null ? "" : new JSONArray(roles).toString();
    }

    @Override
    public String getServiceRealm() {
        RealmContext realm = serviceContext.getServiceRealm();
        if (realm != null) {
            return realm.getName();
        }
        return "";
    }

    private static JSONObject asJSONObject(ServiceProperties properties) {
        JSONObject result = new JSONObject();
        try {
            for (String name : properties.simplePropertyNames()) {
                result.put(name, properties.get(name));
            }
            for (String name : properties.nestedPropertyNames()) {
                for (ServiceProperties nested : properties.getNested(name)) {
                    result.append(name, asJSONObject(nested));
                }
            }
        } catch (JSONException e) {
            // can't happen (unless ServiceProperties has a bug and incorrectly returns a null property name)
            throw new RuntimeException(e);
        }
        return result;
    }
}
