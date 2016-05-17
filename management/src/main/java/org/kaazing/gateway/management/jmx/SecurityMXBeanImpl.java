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
import org.kaazing.gateway.management.config.SecurityConfigurationBean;

public class SecurityMXBeanImpl implements SecurityMXBean {
    private final ObjectName name;
    private final SecurityConfigurationBean securityBean;

    public SecurityMXBeanImpl(ObjectName name, SecurityConfigurationBean securityBean) {
        this.name = name;
        this.securityBean = securityBean;
    }

    public ObjectName getObjectName() {
        return name;
    }

    @Override
    public String getKeystoreType() {
        return securityBean.getKeystoreType();
    }

    @Override
    public String getKeystoreCertificateInfo() {
        return securityBean.getKeystoreCertificateInfo();
    }

    @Override
    public String getTruststoreType() {
        return securityBean.getTruststoreType();
    }

    @Override
    public String getTruststoreCertificateInfo() {
        return securityBean.getTruststoreCertificateInfo();
    }
}
