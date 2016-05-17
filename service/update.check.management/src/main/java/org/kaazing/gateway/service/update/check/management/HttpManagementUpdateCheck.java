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

import org.kaazing.gateway.management.update.check.ManagementUpdateCheck;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.update.check.GatewayVersion;
import org.kaazing.gateway.service.update.check.UpdateCheckListener;
import org.kaazing.gateway.service.update.check.UpdateCheckService;

public class HttpManagementUpdateCheck implements UpdateCheckListener, ManagementUpdateCheck {

    private UpdateCheckService updateCheckService;
    private GatewayVersion latestVersion;

    @Override
    public void newVersionAvailable(GatewayVersion currentVersion, GatewayVersion latestGatewayVersion) {
        if (latestVersion == null || latestVersion.compareTo(latestGatewayVersion) < 0) {
            this.latestVersion = latestGatewayVersion;
        }
    }

    @Override
    public void checkForUpdate() {
        if (updateCheckService != null) {
            updateCheckService.checkForUpdate(this);
        } else {
            // Requirement to run check even is service isn't running
            Service service = ServiceFactory.newServiceFactory().newService("update.check");
            if (service != null) {
                updateCheckService = (UpdateCheckService) service;
                updateCheckService.checkForUpdate(this);
            }
        }
    }

    @Override
    public String getAvailableUpdateVersion() {
        return (this.latestVersion != null) ? this.latestVersion.toString() : "";
    }

    @Override
    public void setUpdateCheckService(UpdateCheckService service) {
        this.updateCheckService = service;
    }

}
