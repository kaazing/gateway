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
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

public class VersionInfoMXBeanImpl implements VersionInfoMXBean {

    private final GatewayManagementBean gatewayManagementBean;

    public VersionInfoMXBeanImpl(ObjectName name, GatewayManagementBean gatewayManagementBean) {
        this.gatewayManagementBean = gatewayManagementBean;
    }

    @Override
    public String getProductTitle() {
        return gatewayManagementBean.getProductTitle();
    }

    @Override
    public String getProductBuild() {
        return gatewayManagementBean.getProductBuild();
    }

    @Override
    public String getProductEdition() {
        return gatewayManagementBean.getProductEdition();
    }
}
