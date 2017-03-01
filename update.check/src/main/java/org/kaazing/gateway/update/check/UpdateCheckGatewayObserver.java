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
package org.kaazing.gateway.update.check;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductEdition;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductTitle;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersionPatch;
import static org.kaazing.gateway.update.check.GatewayVersion.parseGatewayVersion;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.kaazing.gateway.server.GatewayObserverFactorySpiPrototype;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and manages periodic checks to see if the gateway has updates
 */
public class UpdateCheckGatewayObserver extends GatewayObserverFactorySpiPrototype {

    private static final String URL_ENTERPRISE = "https://version.kaazing.com";
    private static final String URL_COMMUNITY = "https://version.kaazing.org";
    private final static Logger LOG = LoggerFactory.getLogger(UpdateCheckGatewayObserver.class);
    private GatewayVersion currentVersion;
    private String productName;
    private String versionServiceUrl;
    private Set<UpdateCheckListener> listeners = new HashSet<>();
    private GatewayVersion latestVersion;

    public UpdateCheckGatewayObserver() {
        addListener(new UpdateCheckLoggingListener());
    }

    /**
     * @return latest @GatewayVersion or null if the latest version has not been discovered
     */
    private synchronized GatewayVersion getLatestGatewayVersion() {
        return latestVersion;
    }

    protected void setLatestGatewayVersion(GatewayVersion newlatestVersion) {
        if (this.latestVersion == null || this.latestVersion.compareTo(newlatestVersion) < 0) {
            this.latestVersion = newlatestVersion;
            if (newlatestVersion.compareTo(currentVersion) <= 0) {
                return;
            }
            for (UpdateCheckListener listener : listeners) {
                listener.newVersionAvailable(currentVersion, getLatestGatewayVersion());
            }
        }
    }

    /**
     * Adds an @UpdateCheckListener who will be notified when the version changes,
     * @param newListener
     */
    public void addListener(UpdateCheckListener newListener) {
        if (newListener == null) {
            throw new NullPointerException("newListener");
        }
        GatewayVersion latestGatewayVersion = this.getLatestGatewayVersion();
        if (latestGatewayVersion != null && latestGatewayVersion.compareTo(currentVersion) > 0) {
            newListener.newVersionAvailable(currentVersion, latestGatewayVersion);
        }
        listeners.add(newListener);
    }

    @Override
    public void startingGateway(GatewayContext gatewayContext) {
        Properties properties = (Properties) gatewayContext.getInjectables().get("configuration");
        if (!InternalSystemProperty.UPDATE_CHECK.getBooleanProperty(properties)) {
            return;
        }

        productName = getGatewayProductTitle().replaceAll("\\s+", "");
        try {
            currentVersion = parseGatewayVersion(getGatewayProductVersionPatch());
        } catch (Exception e) {
            LOG.warn("Could not locate a product version associated with the jars on the classpath", e);
            return;
        }

        String serviceUrl = InternalSystemProperty.SERVICE_URL.getProperty(properties);
        if (serviceUrl != null) {
            versionServiceUrl = serviceUrl;
        } else {
            final String productEdition = getGatewayProductEdition().replaceAll("\\s+", "");
            versionServiceUrl = (productEdition.toLowerCase().contains("enterprise")) ? URL_ENTERPRISE : URL_COMMUNITY;
        }

        try {
            SchedulerProvider provider = (SchedulerProvider) gatewayContext.getInjectables().get("schedulerProvider");
            ScheduledExecutorService scheduler = provider.getScheduler("update_check", false);
            UpdateCheckTask updateCheckTask = new UpdateCheckTask(this, versionServiceUrl, productName);
            scheduler.scheduleAtFixedRate(updateCheckTask, 0, 7, DAYS);
        } catch (Exception e) {
            LOG.warn("An exception occurred while checking for a new product version", e);
        }
    }
}
