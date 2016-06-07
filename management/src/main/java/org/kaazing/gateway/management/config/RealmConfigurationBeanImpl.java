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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.security.RealmContext;

public class RealmConfigurationBeanImpl implements RealmConfigurationBean {

    private final RealmContext realm;
    private final GatewayManagementBean gatewayBean;
    private final int id;
    private static final AtomicInteger staticIdCounter = new AtomicInteger(0);

    public RealmConfigurationBeanImpl(RealmContext realm, GatewayManagementBean gatewayBean) {
        this.realm = realm;
        this.gatewayBean = gatewayBean;
        this.id = staticIdCounter.incrementAndGet();
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
    public String getAuthorizationMode() {
        return realm.getAuthenticationContext().getAuthorizationMode();
    }

    @Override
    public String getSessionTimeout() {
        return realm.getAuthenticationContext().getSessionTimeout();
    }

    @Override
    public String getLoginModules() {
        Configuration configuration = realm.getConfiguration();
        AppConfigurationEntry[] appConfigEntries = configuration.getAppConfigurationEntry(realm.getName());

        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObj;

        try {
            for (AppConfigurationEntry appConfigEntry : appConfigEntries) {
                String loginModuleName = appConfigEntry.getLoginModuleName();
                LoginModuleControlFlag flag = appConfigEntry.getControlFlag();
                Map<String, ?> options = appConfigEntry.getOptions();
                jsonObj = new JSONObject();
                jsonObj.put("type", loginModuleName);
                // success enum values do a 'toString' that includes the type
                // before the value. Send over only actual value.
                String successVal = flag.toString();
                successVal = successVal.substring(successVal.indexOf(" ") + 1);
                jsonObj.put("success", successVal.toLowerCase());
                if ((options != null) && !options.isEmpty()) {
                    jsonObj.put("options", options);
                }
                jsonArray.put(jsonObj);
            }
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonArray.toString();
    }

    @Override
    public String getDescription() {
        return realm.getDescription();
    }

    @Override
    public String getHTTPChallengeScheme() {
        return realm.getAuthenticationContext().getHttpChallengeScheme();
    }

    @Override
    public String getHTTPCookieNames() {
        return makeJSONArray(realm.getAuthenticationContext().getHttpCookieNames());
    }

    @Override
    public String getHTTPHeaders() {
        return makeJSONArray(realm.getAuthenticationContext().getHttpHeaders());
    }

    @Override
    public String getHTTPQueryParameters() {
        return makeJSONArray(realm.getAuthenticationContext().getHttpQueryParameters());
    }

    @Override
    public String getName() {
        return realm.getName();
    }

    @Override
    public String getUserPrincipalClasses() {
        return makeJSONArray(realm.getUserPrincipalClasses());
    }

    public String makeJSONArray(String[] strArray) {
        if (strArray == null) {
            return null;
        } else {
            try {
                JSONArray jsonArray = new JSONArray(strArray);
                return jsonArray.toString();
            } catch (Exception ex) {
                // This is only for JSON exceptions, but there should be no way to
                // hit this.
                return null;
            }
        }
    }
}
