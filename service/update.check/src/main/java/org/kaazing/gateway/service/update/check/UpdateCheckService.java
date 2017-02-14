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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductEdition;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductTitle;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersionPatch;
import static org.kaazing.gateway.service.update.check.GatewayVersion.parseGatewayVersion;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.Resource;

import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Creates and manages periodic checks to see if the gateway has updates
 * 
 */
public class UpdateCheckService implements Service {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTasks;

    private GatewayVersion latestVersion;
    private final GatewayVersion currentVersion;
    private final String productName;
    private final String versionServiceUrl;
    private final Set<UpdateCheckListener> listeners = new HashSet<>();
    private ServiceContext serviceContext;

    public static String MANAGEMENT_UPDATE_CHECK_LISTENER = "updateCheckListeners";

    public UpdateCheckService() {
        productName = getGatewayProductTitle().replaceAll("\\s+", "");
        try {
            currentVersion = parseGatewayVersion(getGatewayProductVersionPatch());
        } catch (Exception e) {
            throw new RuntimeException("Could not locate a product version associated with the jars on the classpath",
                    e);
        }
        final String productEdition = getGatewayProductEdition().replaceAll("\\s+", "");
        versionServiceUrl = (productEdition.toLowerCase().contains("enterprise")) ? "https://version.kaazing.com"
                : "https://version.kaazing.org";
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("update_check_service", false);
    }

    @Override
    public String getType() {
        return "update check";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
    }

    @Override
    public void start() throws Exception {
        // add listeners
        listeners.clear();
        addListener(new UpdateCheckLoggingListener());
        Map<String, Object> serviceSpecificObjects = serviceContext.getServiceSpecificObjects();
        Object managementListener = serviceSpecificObjects.get(MANAGEMENT_UPDATE_CHECK_LISTENER);
        if (managementListener != null && managementListener instanceof UpdateCheckListener) {
            addListener((UpdateCheckListener) managementListener);
        }
        for (UpdateCheckListener listener : listeners) {
            listener.setUpdateCheckService(this);
        }
        scheduledTasks = scheduler.scheduleAtFixedRate(new UpdateCheckTask(this, versionServiceUrl, productName), 0, 7,
                DAYS);
    }

    @Override
    public void stop() throws Exception {
        scheduledTasks.cancel(false);
    }

    @Override
    public void quiesce() throws Exception {
        scheduledTasks.cancel(false);
    }

    @Override
    public void destroy() throws Exception {
    	// may be null if start was not called (exception in init)
    	if (scheduledTasks != null)
    		scheduledTasks.cancel(true);
    }

    /**
     * Forces a check for an update and registers the listener if it is not already registered
     * @param updateCheckListener
     */
    public void checkForUpdate(UpdateCheckListener updateCheckListener) {
        listeners.add(updateCheckListener);
        if (scheduler != null) {
            scheduler.schedule(new UpdateCheckTask(this, versionServiceUrl, productName), 0, SECONDS);
        } else {
            // the scheduler won't be provided if the service isn't actually running,
            // but management may still ask for a check on update
            new UpdateCheckTask(this, versionServiceUrl, productName).run();
        }
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

    /**
     * @return latest @GatewayVersion or null if the latest version has not been discovered
     */
    private synchronized GatewayVersion getLatestGatewayVersion() {
        return latestVersion;
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
     * @param newListener
     */
    public void addListener(UpdateCheckListener newListener) {
        GatewayVersion latestGatewayVersion = this.getLatestGatewayVersion();
        if (latestGatewayVersion != null && latestGatewayVersion.compareTo(currentVersion) > 0) {
            newListener.newVersionAvailable(currentVersion, latestGatewayVersion);
        }
        newListener.setUpdateCheckService(this);
        listeners.add(newListener);
    }
}
