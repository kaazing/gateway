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
import static org.kaazing.gateway.service.update.check.GatewayVersion.parseGatewayVersion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An update check task that updates the latest version
 */
public class UpdateCheckTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(UpdateCheckTask.class);
    private final UpdateCheckService updateCheckService;
    private final String protocolVersion = "1.0";
    private final String versionServiceUrl;

    /**
     * @param updateCheckService
     * @param webServiceUrl
     * @param productName
     */
    public UpdateCheckTask(UpdateCheckService updateCheckService, String webServiceUrl, String productName) {
        this.updateCheckService = updateCheckService;
        if (webServiceUrl.endsWith("/")) {
            this.versionServiceUrl = format("%s%s/%s/latest", webServiceUrl, productName, protocolVersion);
        } else {
            this.versionServiceUrl = format("%s/%s/%s/latest", webServiceUrl, productName, protocolVersion);
        }
    }

    @Override
    public void run() {
        GatewayVersion latestVersion = fetchLatestVersion();
        if (latestVersion != null) {
            updateCheckService.setLatestGatewayVersion(latestVersion);
        }
    }

    /**
     * Checks Kaazing Version Web Service to see what is the latest version of the product
     * @return null if an exception is caught, otherwise returns the latest version of the gateway
     */
    private GatewayVersion fetchLatestVersion() {
        GatewayVersion latestVersion = null;
        String updateVersionUrl = getVersionServiceUrl();
        try {
            URL url = new URL(updateVersionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Kaazing Update Service");
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Accept", "text/plain");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode <= 300) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    JSONTokener tokener = new JSONTokener(br);
                    JSONObject root = new JSONObject(tokener);
                    String version = root.getString("version");
                    latestVersion = parseGatewayVersion(version);
                }
            } else {
                throw new Exception(format("Unexpected %d response code from versioning service", responseCode));
            }
        } catch (Exception e) {
            logger.warn(format(
                    "Update Check Service: Could not contact Kaazing versioning service at %s to find latest version of product: %s",
                    updateVersionUrl, e));
        }
        return latestVersion;
    }

    /**
     * Here for testing, thus not public
     * @return
     */
    protected String getVersionServiceUrl() {
        return versionServiceUrl;
    }
}
