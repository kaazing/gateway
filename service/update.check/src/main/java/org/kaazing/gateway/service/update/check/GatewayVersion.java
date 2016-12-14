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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represent GatewayVersion with 3 digits of form major.minor.patch
 * 
 */
public class GatewayVersion implements Comparable<GatewayVersion> {

    private static final String RELEASE_GA = "";

    private final int major;
    private final int minor;
    private final int patch;
    private final String rc;

    public GatewayVersion(int major, int minor, int patch, String rc) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.rc = rc;
    }

    public GatewayVersion(int major, int minor, int patch) {
        this(major, minor, patch, RELEASE_GA);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getRc() {
        return rc;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result;
        if (!(obj instanceof GatewayVersion)) {
            result = false;
        } else if (obj == this) {
            result = true;
        } else {
            result = this.compareTo((GatewayVersion) obj) == 0;
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        result = 31 * result + (rc != null ? rc.hashCode() : 0);
        return result;
    }

    /**
     * Parses a GatewayVersion from a String
     * @param version
     * @return
     * @throws Exception
     */
    public static GatewayVersion parseGatewayVersion(String version) throws Exception {
        if ("develop-SNAPSHOT".equals(version)) {
            return new GatewayVersion(0, 0, 0);
        } else {
            String regex = "(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)-?(?<rc>[RC0-9{3}]*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(version);
            if (matcher.matches()) {
                int major = Integer.parseInt(matcher.group("major"));
                int minor = Integer.parseInt(matcher.group("minor"));
                int patch = Integer.parseInt(matcher.group("patch"));
                String rc = matcher.group("rc");
                return new GatewayVersion(major, minor, patch, rc);
            } else {
                throw new IllegalArgumentException(String.format("version String is not of form %s", regex));
            }
        }
    }

    @Override
    public String toString() {
        if (rc.equals(RELEASE_GA)) {
            return format("%d.%d.%d", major, minor, patch);
        } else {
            return format("%d.%d.%d-%s", major, minor, patch, rc);
        }
    }

    @Override
    public int compareTo(GatewayVersion o) {
        int result;
        if (this.major != o.major) {
            result = this.major > o.major ? 1 : -1;
        } else if (this.minor != o.minor) {
            result = this.minor > o.minor ? 1 : -1;
        } else if (this.patch != o.patch) {
            result = this.patch > o.patch ? 1 : -1;
        } else if (!this.rc.equals(o.rc)) {
            result = RELEASE_GA.equals(this.rc)?1:(RELEASE_GA.equals(o.rc)?-1:this.rc.compareTo(o.rc));
        } else {
            result = 0;
        }
        return result;
    }
}
