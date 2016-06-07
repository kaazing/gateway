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
package org.kaazing.gateway.management.jmx;


import javax.management.ObjectName;
import org.kaazing.gateway.management.config.RealmConfigurationBean;

public class RealmMXBeanImpl implements RealmMXBean {

    private ObjectName name;
    private RealmConfigurationBean realmBean;

    public RealmMXBeanImpl(ObjectName name, RealmConfigurationBean realmBean) {
        this.name = name;
        this.realmBean = realmBean;
    }

    public ObjectName getObjectName() {
        return name;
    }

    @Override
    public String getAuthorizationMode() {
        return realmBean.getAuthorizationMode();
    }

    @Override
    public String getSessionTimeout() {
        return realmBean.getSessionTimeout();
    }

    @Override
    public String getLoginModules() {
        return realmBean.getLoginModules();
    }

    @Override
    public String getDescription() {
        return realmBean.getDescription();
    }

    @Override
    public String getHTTPChallengeScheme() {
        return realmBean.getHTTPChallengeScheme();
    }

    @Override
    public String getHTTPCookieNames() {
        return realmBean.getHTTPCookieNames();
    }

    @Override
    public String getHTTPHeaders() {
        return realmBean.getHTTPHeaders();
    }

    @Override
    public String getHTTPQueryParameters() {
        return realmBean.getHTTPQueryParameters();
    }

    @Override
    public int getId() {
        return realmBean.getId();
    }

    @Override
    public String getName() {
        return realmBean.getName();
    }

    @Override
    public String getUserPrincipalClasses() {
        return realmBean.getUserPrincipalClasses();
    }
}
