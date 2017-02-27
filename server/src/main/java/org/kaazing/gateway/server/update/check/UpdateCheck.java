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
package org.kaazing.gateway.server.update.check;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductEdition;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductTitle;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersionPatch;
import static org.kaazing.gateway.server.update.check.GatewayVersion.parseGatewayVersion;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and manages periodic checks to see if the gateway has updates
 */
public class UpdateCheck {

    private final GatewayVersion currentVersion;
    private final String productName;
    private final String versionServiceUrl;
    private final Set<UpdateCheckListener> listeners = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(UpdateCheckTask.class);
    private ScheduledExecutorService scheduler;
    private GatewayVersion latestVersion;


    public UpdateCheck(Properties configuration) {
        productName = getGatewayProductTitle().replaceAll("\\s+", "");
        try {
            currentVersion = parseGatewayVersion(getGatewayProductVersionPatch());
        } catch (Exception e) {
            throw new RuntimeException("Could not locate a product version associated with the jars on the classpath",
                    e);
        }
        String serviceUrl = InternalSystemProperty.SERVICE_URL.getProperty(configuration);

        if (serviceUrl != null)
            versionServiceUrl = serviceUrl;
        else {
            final String productEdition = getGatewayProductEdition().replaceAll("\\s+", "");
            versionServiceUrl = (productEdition.toLowerCase().contains("enterprise")) ? "https://version.kaazing.com"
                    : "https://version.kaazing.org";
        }

    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("update_check", false);
    }


    public void start() throws Exception {
        listeners.clear();
        addListener(new UpdateCheckLoggingListener());
        for (UpdateCheckListener listener : listeners) {
            listener.setUpdateCheck(this);
        }
        UpdateCheckTask updateCheckTask = new UpdateCheckTask(this, versionServiceUrl, productName);
        scheduler.scheduleAtFixedRate(updateCheckTask, 0, 7,
                DAYS);
    }

    /**
     * @return latest @GatewayVersion or null if the latest version has not been discovered
     */
    private synchronized GatewayVersion getLatestGatewayVersion() {
        return latestVersion;
    }

    protected void setLatestGatewayVersion(GatewayVersion newlatestVersion) {

        if (this.latestVersion == null || this.latestVersion.compareTo(newlatestVersion) < 0) {
            synchronized (this) {
                this.latestVersion = newlatestVersion;
            }
            if (newlatestVersion.compareTo(currentVersion) > 0) {
                notifyListeners();
            }
        }
    }

    private void notifyListeners() {

        for (UpdateCheckListener listener : listeners) {

            if (listener != null) {
                listener.newVersionAvailable(currentVersion, getLatestGatewayVersion());
            }
        }
    }

    /**
     * Adds a @UpdateCheckListener who will be notified when the version changes,
     *
     * @param newListener
     */
    public void addListener(UpdateCheckListener newListener) {
        GatewayVersion latestGatewayVersion = this.getLatestGatewayVersion();
        if (latestGatewayVersion != null && latestGatewayVersion.compareTo(currentVersion) > 0) {
            newListener.newVersionAvailable(currentVersion, latestGatewayVersion);
        }
        newListener.setUpdateCheck(this);
        listeners.add(newListener);
    }
}
