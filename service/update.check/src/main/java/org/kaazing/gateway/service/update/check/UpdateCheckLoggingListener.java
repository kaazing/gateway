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
package org.kaazing.gateway.service.update.check;

import static java.lang.String.format;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductTitle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On notifications of new versions, this class logs the information
 * 
 */
class UpdateCheckLoggingListener implements UpdateCheckListener {

    private final Logger logger = LoggerFactory.getLogger(UpdateCheckTask.class);

    @Override
    public void newVersionAvailable(GatewayVersion currentVersion, GatewayVersion latestGatewayVersion) {
        logger.info(format(
                "Update Check Service: New release available for download: %s %s (you are currently running %s)",
                getGatewayProductTitle(), latestGatewayVersion, currentVersion));
    }

    @Override
    public void setUpdateCheckService(UpdateCheckService service) {
        // NOOP, don't care about the service
    }
}