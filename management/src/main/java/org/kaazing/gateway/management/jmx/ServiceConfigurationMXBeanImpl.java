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
import org.kaazing.gateway.management.config.ServiceConfigurationBean;

public class ServiceConfigurationMXBeanImpl implements ServiceConfigurationMXBean {

    private final ServiceConfigurationBean serviceConfigurationBean;

    public ServiceConfigurationMXBeanImpl(ObjectName name, ServiceConfigurationBean serviceConfigurationBean) {
        this.serviceConfigurationBean = serviceConfigurationBean;
    }

    @Override
    public int getId() {
        return serviceConfigurationBean.getId();
    }

    @Override
    public String getType() {
        return serviceConfigurationBean.getType();
    }

    @Override
    public String getServiceName() {
        return serviceConfigurationBean.getServiceName();
    }

    @Override
    public String getServiceDescription() {
        return serviceConfigurationBean.getServiceDescription();
    }

    @Override
    public String getAccepts() {
        return serviceConfigurationBean.getAccepts();
    }

    @Override
    public String getAcceptOptions() {
        return serviceConfigurationBean.getAcceptOptions();
    }

    @Override
    public String getBalances() {
        return serviceConfigurationBean.getBalances();
    }

    @Override
    public String getConnects() {
        return serviceConfigurationBean.getConnects();
    }

    @Override
    public String getConnectOptions() {
        return serviceConfigurationBean.getConnectOptions();
    }

    @Override
    public String getCrossSiteConstraints() {
        return serviceConfigurationBean.getCrossSiteConstraints();
    }

    @Override
    public String getProperties() {
        return serviceConfigurationBean.getProperties();
    }

    @Override
    public String getRequiredRoles() {
        return serviceConfigurationBean.getRequiredRoles();
    }

    @Override
    public String getServiceRealm() {
        return serviceConfigurationBean.getServiceRealm();
    }

    @Override
    public String getMimeMappings() {
        return serviceConfigurationBean.getMimeMappings();
    }
}
