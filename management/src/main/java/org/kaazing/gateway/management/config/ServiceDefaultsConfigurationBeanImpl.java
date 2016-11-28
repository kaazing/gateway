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
package org.kaazing.gateway.management.config;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.server.context.ServiceDefaultsContext;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.util.Utils;

@SuppressWarnings("deprecation")
public class ServiceDefaultsConfigurationBeanImpl implements ServiceDefaultsConfigurationBean {

    private static final AtomicInteger serviceDefaultsIds = new AtomicInteger(0);
    private final ServiceDefaultsContext serviceDefaultsContext;
    private final GatewayManagementBean gatewayBean;
    private final int id;

    public ServiceDefaultsConfigurationBeanImpl(ServiceDefaultsContext serviceDefaultsContext, GatewayManagementBean
            gatewayBean) {
        this.serviceDefaultsContext = serviceDefaultsContext;
        this.gatewayBean = gatewayBean;
        this.id = serviceDefaultsIds.incrementAndGet();
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayBean;
    }

    @Override
    public int getId() {
        return id;
    }

    // Note: the following is copied and modified from ServiceConfigurationBeanImpl
    @Override
    public String getAcceptOptions() {
        JSONObject jsonOptions = new JSONObject();
        JSONObject jsonObj;

        AcceptOptionsContext context = serviceDefaultsContext.getAcceptOptionsContext();
        try {
            if (context != null) {
                Map<String, Object> acceptOptions = context.asOptionsMap();
                Map<String, String> binds = context.getBinds();
                if ((binds != null) && !binds.isEmpty()) {
                    jsonObj = new JSONObject();
                    for (String key : binds.keySet()) {
                        jsonObj.put(key, binds.get(key));
                    }
                    jsonOptions.put("binds", jsonObj);
                }

                String[] sslCiphers = (String[]) acceptOptions.remove("ssl.ciphers");
                if (sslCiphers != null && sslCiphers.length > 0) {
                    jsonOptions.put("ssl.ciphers", Utils.asCommaSeparatedString(asList(sslCiphers)));
                }

                boolean isSslEncryptionEnabled = (Boolean) acceptOptions.remove("ssl.encryptionEnabled");
                jsonOptions.put("ssl.encryption",
                        isSslEncryptionEnabled ? "enabled" : "disabled");

                boolean wantClientAuth = (Boolean) acceptOptions.remove("ssl.wantClientAuth");
                boolean needClientAuth = (Boolean) acceptOptions.remove("ssl.needClientAuth");
                if (needClientAuth) {
                    jsonOptions.put("ssl.verify-client", "required");
                } else if (wantClientAuth) {
                    jsonOptions.put("ssl.verify-client", "optional");
                } else {
                    jsonOptions.put("ssl.verify-client", "none");
                }

                jsonOptions.put("ws.maximum.message.size", acceptOptions.remove("ws.maxMessageSize"));

                Integer httpKeepAlive = (Integer) acceptOptions.remove("http[http/1.1].keepAliveTimeout");
                if (httpKeepAlive != null) {
                    jsonOptions.put("http.keepalive.timeout", httpKeepAlive);
                }

                String pipeTransport = (String) acceptOptions.remove("pipe.transport");
                if (pipeTransport != null) {
                    jsonOptions.put("pipe.transport", pipeTransport);
                }

                String tcpTransport = (String) acceptOptions.remove("tcp.transport");
                if (tcpTransport != null) {
                    jsonOptions.put("tcp.transport", tcpTransport);
                }

                String sslTransport = (String) acceptOptions.remove("ssl.transport");
                if (sslTransport != null) {
                    jsonOptions.put("ssl.transport", sslTransport);
                }

                String httpTransport = (String) acceptOptions.remove("http.transport");
                if (httpTransport != null) {
                    jsonOptions.put("http.transport", httpTransport);
                }

                long tcpMaxOutboundRate = (Long) acceptOptions.remove("tcp.maximumOutboundRate");
                jsonOptions.put("tcp.maximum.outbound.rate", tcpMaxOutboundRate);

                for (Entry<String, Object> entry : acceptOptions.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("ws") &&
                            (key.endsWith("maxMessageSize") ||
                             key.endsWith("inactivityTimeout") ||
                             key.endsWith("extensions"))) {
                        // skip over options already seen with the base ws.*
                        continue;
                    }

                    Object value = entry.getValue();
                    if (value instanceof String[]) {
                        jsonOptions.put(key, Utils.asCommaSeparatedString(asList((String[]) value)));
                    } else {
                        jsonOptions.put(key, value);
                    }
                }
            }
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonOptions.toString();
    }

    @Override
    public String getConnectOptions() {
        ConnectOptionsContext context = serviceDefaultsContext.getConnectOptionsContext();

        JSONObject jsonOptions = new JSONObject();

        try {
            if (context != null) {
                Map<String, Object> connectOptions = context.asOptionsMap();

                String[] sslCiphersArray = (String[]) connectOptions.remove("ssl.ciphers");
                if (sslCiphersArray != null) {
                    List<String> sslCiphers = Arrays.asList(sslCiphersArray);
                    if (sslCiphers.size() > 0) {
                        jsonOptions.put("ssl.ciphers", sslCiphers);
                    }
                }

                // NOTE: we do NOT (at least in 4.0) show the WS extensions
                // or WS protocols to users (Command Center or otherwise), so don't send them out.
                // WebSocketWireProtocol protocol = connectOptions.getWebSocketWireProtocol();
                // sb.append("websocket-wire-protocol=" + protocol);

                String wsVersion = (String) connectOptions.remove("ws.version");
                if (wsVersion != null) {
                    jsonOptions.put("ws.version", wsVersion);
                }

                String pipeTransport = (String) connectOptions.remove("pipe.transport");
                if (pipeTransport != null) {
                    jsonOptions.put("pipe.transport", pipeTransport);
                }

                String tcpTransport = (String) connectOptions.remove("tcp.transport");
                if (tcpTransport != null) {
                    jsonOptions.put("tcp.transport", tcpTransport);
                }

                String sslTransport = (String) connectOptions.remove("ssl.transport");
                if (sslTransport != null) {
                    jsonOptions.put("ssl.transport", sslTransport);
                }

                String httpTransport = (String) connectOptions.remove("http.transport");
                if (httpTransport != null) {
                    jsonOptions.put("http.transport", httpTransport);
                }

                for (Entry<String, Object> entry : connectOptions.entrySet()) {
                    String key = entry.getKey();

                    Object value = entry.getValue();
                    if (value instanceof String[]) {
                        jsonOptions.put(key, Utils.asCommaSeparatedString(asList((String[]) value)));
                    } else {
                        jsonOptions.put(key, value);
                    }
                }
            }
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonOptions.toString();
    }

    @Override
    public String getMimeMappings() {
        JSONObject jsonObj = new JSONObject();

        try {
            Map<String, String> mimeMappings = serviceDefaultsContext.getMimeMappings();

            if (mimeMappings != null) {
                for (String extension : mimeMappings.keySet()) {
                    jsonObj.put(extension, mimeMappings.get(extension));
                }
            }
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonObj.toString();
    }
}
