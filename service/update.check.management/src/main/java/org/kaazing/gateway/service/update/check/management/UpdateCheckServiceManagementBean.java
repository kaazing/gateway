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
package org.kaazing.gateway.service.update.check.management;

import static org.kaazing.gateway.service.update.check.UpdateCheckService.MANAGEMENT_UPDATE_CHECK_LISTENER;

import java.util.Map;

import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBean.DefaultServiceManagementBean;
import org.kaazing.gateway.management.update.check.ManagementUpdateCheck;
import org.kaazing.gateway.service.ServiceContext;

public class UpdateCheckServiceManagementBean extends DefaultServiceManagementBean {

    public UpdateCheckServiceManagementBean(GatewayManagementBean gatewayManagementBean, ServiceContext serviceContext) {
        super(gatewayManagementBean, serviceContext);

        final ManagementUpdateCheck updateCheckListener = gatewayManagementBean.getUpdateCheck();
        if (updateCheckListener != null) {
            assert (serviceContext.getServiceType().equals("update.check"));
            Map<String, Object> serviceSpecificObjects = serviceContext.getServiceSpecificObjects();
            serviceSpecificObjects.put(MANAGEMENT_UPDATE_CHECK_LISTENER, updateCheckListener);
        }
    }

}
