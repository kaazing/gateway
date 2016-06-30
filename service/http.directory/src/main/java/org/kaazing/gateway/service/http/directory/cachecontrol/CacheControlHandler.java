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
package org.kaazing.gateway.service.http.directory.cachecontrol;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for caching the cache-control information for each requested file
 */
public class CacheControlHandler {

    private static final String DIRECTIVES_SEPARATOR = ", ";
    private static final String EQUALS_STRING = "=";
    private static final String EMPTY_STRING_VALUE = "";

    private File requestFile;
    private StringBuilder staticDirectives = new StringBuilder();
    private Map<Directive, String> maxAgeDirectives = new HashMap<>();

    private boolean isMaxAgeResolved;

    private long maxAgeResolvedValue;

    public CacheControlHandler(File requestedFile, PatternCacheControl patternCacheControl) {
        this.requestFile = requestedFile;
        buildDirectives(patternCacheControl);
    }

    /**
     * Returns the associated file
     * @return file
     */
    public File getRequestFile() {
        return requestFile;
    }

    /**
     * Returns the Cache-control header string
     * @return
     */
    public String getCacheControlHeader() {
        checkIfMaxAgeIsResolved();
        String maxAge = Directive.MAX_AGE.getName() + "=" + (maxAgeResolvedValue > 0 ? maxAgeResolvedValue : "0");
        return staticDirectives.toString() + maxAge;
    }

    /**
     * Returns the value for the Expires header
     * @return Expires header value
     */
    public long getExpiresHeader() {
        checkIfMaxAgeIsResolved();
        long currentTimeMillis = System.currentTimeMillis();
        return maxAgeResolvedValue * 1000 + currentTimeMillis;
    }

    /**
     * Resets the flag for the resolved max-age value to false
     */
    public void resetState() {
        isMaxAgeResolved = false;
        maxAgeResolvedValue = 0;
    }

    /**
     * Checks if the value for the max-age header has been resolved for the current request
     */
    private void checkIfMaxAgeIsResolved() {
        if (!isMaxAgeResolved) {
            resolveMaxAgeConflicts();
        }
    }

    /**
     * Resolves the conflict between a max-age and a max-age (m+N value) directive
     */
    private void resolveMaxAgeConflicts() {
        long lastModified = requestFile.lastModified();
        String maxAgeString = EMPTY_STRING_VALUE;
        String maxAge = maxAgeDirectives.get(Directive.MAX_AGE);
        String maxAgeMPlus = maxAgeDirectives.get(Directive.MAX_AGE_MPLUS);

        if (maxAgeMPlus != null) {
            String maxAgeMPlusValue = parseMaxAgeMplus(maxAgeMPlus, lastModified);
            if (maxAge != null) {
                maxAgeString = Long.parseLong(maxAge) < Long.parseLong(maxAgeMPlusValue) ? maxAge : maxAgeMPlusValue;
            } else {
                maxAgeString = maxAgeMPlusValue;
            }
        } else if (maxAge != null) {
            maxAgeString = maxAge;
        }
        maxAgeResolvedValue = Long.parseLong(maxAgeString);
        isMaxAgeResolved = true;
    }

    /**
     * Parse value for a max-age (m + N value), according to the requested file's last modified date 
     * @param value
     * @param lastModified
     * @return max-age parsed value
     */
    private String parseMaxAgeMplus(String value, long lastModified) {
        long currentTimeMillis = System.currentTimeMillis();
        return Long.toString(Long.parseLong(value) + (lastModified - currentTimeMillis) / 1000);
    }

    /**
     * Builds a string of directives by concatenating all the static directives
     * @param patternCacheControl
     */
    private void buildDirectives(PatternCacheControl patternCacheControl) {
        for (Map.Entry<Directive, String> entry : patternCacheControl.getDirectives().entrySet()) {
            Directive key = entry.getKey();
            String value = entry.getValue();

            switch (key) {
            case MAX_AGE:
            case MAX_AGE_MPLUS:
                maxAgeDirectives.put(key, value);
                break;
            default:
                if (value.equals(EMPTY_STRING_VALUE)) {
                    staticDirectives.append(key.getName() + DIRECTIVES_SEPARATOR);
                    break;
                }
                staticDirectives.append(key.getName() + EQUALS_STRING + value + DIRECTIVES_SEPARATOR);
            }
        }
    }
}
