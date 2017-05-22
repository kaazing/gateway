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
package org.kaazing.gateway.server.util;

public class ProductInfo {

    private String title = "Kaazing WebSocket Gateway (Development)";
    private String version;
    private String edition;
    private String dependencies;
    private String major = "0";
    private String minor = "0";
    private String patch = "0";
    private String build = "0";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
        extractVersionComponents(version);
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    public String getPatch() {
        return patch;
    }

    public String getBuild() {
        return build;
    }

    private void extractVersionComponents(String version) {
        if ("develop-SNAPSHOT".equals(version)) {
            return;
        }
        String[] splits = version.split("\\.");
        if(splits.length >= 1)
            this.major = splits[0];
        if(splits.length >= 2)
            this.minor = splits[1];
        if(splits.length >= 3)
            this.patch = splits[2];
        if(splits.length >= 4)
            this.build = splits[3];
    }
}
